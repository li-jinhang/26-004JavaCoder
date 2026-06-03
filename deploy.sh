#!/usr/bin/env bash
set -Eeuo pipefail

# Linux deployment script for JavaCoder.
# Override settings with environment variables, for example:
#   BRANCH=main BACKEND_SERVICE=javacoder-backend FRONTEND_DEPLOY_DIR=/usr/share/nginx/html ./deploy.sh

APP_DIR="${APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"
REMOTE="${REMOTE:-origin}"
BRANCH="${BRANCH:-}"
ALLOW_DIRTY="${ALLOW_DIRTY:-false}"

BACKEND_DEPLOY_DIR="${BACKEND_DEPLOY_DIR:-/opt/javacoder/backend}"
BACKEND_JAR_NAME="${BACKEND_JAR_NAME:-javacoder-backend.jar}"
BACKEND_SERVICE="${BACKEND_SERVICE:-javacoder-backend}"
BACKEND_RUN_MODE="${BACKEND_RUN_MODE:-systemd}" # systemd, jar, or none
BACKEND_JAVA_OPTS="${BACKEND_JAVA_OPTS:-}"
BACKEND_PID_FILE="${BACKEND_PID_FILE:-/opt/javacoder/backend/javacoder-backend.pid}"
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-/opt/javacoder/backend/javacoder-backend.log}"

FRONTEND_DEPLOY_DIR="${FRONTEND_DEPLOY_DIR:-/var/www/javacoder/frontend/dist}"

BUILD_SANDBOX_IMAGE="${BUILD_SANDBOX_IMAGE:-true}"
SANDBOX_IMAGE_NAME="${SANDBOX_IMAGE_NAME:-javacoder-java17-sandbox:latest}"

SKIP_TESTS="${SKIP_TESTS:-true}"
RUN_NPM_CI="${RUN_NPM_CI:-true}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-http://127.0.0.1:26904/api/health}"
HEALTHCHECK_RETRIES="${HEALTHCHECK_RETRIES:-30}"
USE_SUDO="${USE_SUDO:-auto}"

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

run_sudo() {
  if [[ "${USE_SUDO}" == "true" || ( "${USE_SUDO}" == "auto" && "${EUID}" -ne 0 ) ]]; then
    sudo "$@"
  else
    "$@"
  fi
}

check_prerequisites() {
  need_cmd git
  need_cmd npm
  need_cmd mvn
  need_cmd java
  need_cmd rsync

  if [[ "${BUILD_SANDBOX_IMAGE}" == "true" ]]; then
    need_cmd docker
  fi

  if [[ "${USE_SUDO}" != "false" && "${EUID}" -ne 0 ]]; then
    need_cmd sudo
  fi
}

pull_latest_code() {
  cd "${APP_DIR}"
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "${APP_DIR} is not a git repository"

  if [[ -z "${BRANCH}" ]]; then
    BRANCH="$(git symbolic-ref --short HEAD)"
  fi

  if [[ "${ALLOW_DIRTY}" != "true" && -n "$(git status --porcelain)" ]]; then
    fail "Working tree is not clean. Commit, stash, or set ALLOW_DIRTY=true before deploying."
  fi

  log "Pulling latest code from ${REMOTE}/${BRANCH}"
  git fetch "${REMOTE}" "${BRANCH}"
  git checkout "${BRANCH}"
  git pull --ff-only "${REMOTE}" "${BRANCH}"
}

install_dependencies() {
  if [[ "${RUN_NPM_CI}" == "true" ]]; then
    log "Installing frontend dependencies"
    if [[ -f "${APP_DIR}/frontend/package-lock.json" ]]; then
      npm --prefix "${APP_DIR}/frontend" ci
    else
      npm --prefix "${APP_DIR}/frontend" install
    fi
  fi
}

build_project() {
  log "Building frontend"
  npm --prefix "${APP_DIR}/frontend" run build

  log "Building backend"
  if [[ "${SKIP_TESTS}" == "true" ]]; then
    mvn -f "${APP_DIR}/backend/pom.xml" clean package -DskipTests
  else
    mvn -f "${APP_DIR}/backend/pom.xml" clean package
  fi

  if [[ "${BUILD_SANDBOX_IMAGE}" == "true" ]]; then
    log "Building Java sandbox Docker image: ${SANDBOX_IMAGE_NAME}"
    run_sudo docker build -f "${APP_DIR}/backend/Dockerfile.sandbox-java17" -t "${SANDBOX_IMAGE_NAME}" "${APP_DIR}/backend"
  fi
}

deploy_frontend() {
  log "Deploying frontend to ${FRONTEND_DEPLOY_DIR}"
  [[ -d "${APP_DIR}/frontend/dist" ]] || fail "Frontend build output not found: ${APP_DIR}/frontend/dist"

  run_sudo mkdir -p "${FRONTEND_DEPLOY_DIR}"
  run_sudo rsync -a --delete "${APP_DIR}/frontend/dist/" "${FRONTEND_DEPLOY_DIR}/"
}

find_backend_jar() {
  local jar
  jar="$(find "${APP_DIR}/backend/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*' | head -n 1)"
  [[ -n "${jar}" ]] || fail "Backend jar not found under ${APP_DIR}/backend/target"
  printf '%s\n' "${jar}"
}

deploy_backend_jar() {
  local source_jar target_jar timestamp

  source_jar="$(find_backend_jar)"
  target_jar="${BACKEND_DEPLOY_DIR}/${BACKEND_JAR_NAME}"
  timestamp="$(date '+%Y%m%d%H%M%S')"

  log "Deploying backend jar to ${target_jar}"
  run_sudo mkdir -p "${BACKEND_DEPLOY_DIR}"

  if run_sudo test -f "${target_jar}"; then
    run_sudo cp "${target_jar}" "${target_jar}.bak.${timestamp}"
  fi

  run_sudo install -m 0644 "${source_jar}" "${target_jar}"
}

restart_backend_systemd() {
  need_cmd systemctl
  [[ -n "${BACKEND_SERVICE}" ]] || fail "BACKEND_SERVICE is required when BACKEND_RUN_MODE=systemd"

  log "Restarting systemd service: ${BACKEND_SERVICE}"
  run_sudo systemctl daemon-reload
  run_sudo systemctl restart "${BACKEND_SERVICE}"
}

restart_backend_jar() {
  local target_jar
  target_jar="${BACKEND_DEPLOY_DIR}/${BACKEND_JAR_NAME}"

  log "Restarting backend with java -jar"
  run_sudo mkdir -p "$(dirname "${BACKEND_PID_FILE}")" "$(dirname "${BACKEND_LOG_FILE}")"

  if run_sudo test -f "${BACKEND_PID_FILE}"; then
    local old_pid
    old_pid="$(run_sudo cat "${BACKEND_PID_FILE}" || true)"
    if [[ -n "${old_pid}" ]] && run_sudo kill -0 "${old_pid}" >/dev/null 2>&1; then
      run_sudo kill "${old_pid}" || true
      sleep 3
    fi
  fi

  if [[ "${USE_SUDO}" == "true" || ( "${USE_SUDO}" == "auto" && "${EUID}" -ne 0 ) ]]; then
    sudo sh -c "nohup java ${BACKEND_JAVA_OPTS} -jar '${target_jar}' >> '${BACKEND_LOG_FILE}' 2>&1 & echo \$! > '${BACKEND_PID_FILE}'"
  else
    sh -c "nohup java ${BACKEND_JAVA_OPTS} -jar '${target_jar}' >> '${BACKEND_LOG_FILE}' 2>&1 & echo \$! > '${BACKEND_PID_FILE}'"
  fi
}

restart_backend() {
  case "${BACKEND_RUN_MODE}" in
    systemd)
      restart_backend_systemd
      ;;
    jar)
      restart_backend_jar
      ;;
    none)
      log "Skipping backend restart because BACKEND_RUN_MODE=none"
      ;;
    *)
      fail "Unknown BACKEND_RUN_MODE: ${BACKEND_RUN_MODE}"
      ;;
  esac
}

wait_for_healthcheck() {
  [[ -n "${HEALTHCHECK_URL}" ]] || return 0

  if ! command -v curl >/dev/null 2>&1; then
    log "curl is not installed; skipping health check"
    return 0
  fi

  log "Checking backend health: ${HEALTHCHECK_URL}"
  for ((attempt = 1; attempt <= HEALTHCHECK_RETRIES; attempt++)); do
    if curl -fsS "${HEALTHCHECK_URL}" >/dev/null; then
      log "Health check passed"
      return 0
    fi
    sleep 2
  done

  fail "Health check failed after ${HEALTHCHECK_RETRIES} attempts: ${HEALTHCHECK_URL}"
}

main() {
  check_prerequisites
  pull_latest_code
  install_dependencies
  build_project
  deploy_frontend
  deploy_backend_jar
  restart_backend
  wait_for_healthcheck
  log "Deployment completed"
}

main "$@"
