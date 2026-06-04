#!/usr/bin/env bash
set -Eeuo pipefail

# Linux deployment script for JavaCoder.
# Override settings with environment variables, for example:
#   BACKEND_SERVICE=javacoder-backend FRONTEND_DEPLOY_DIR=/usr/share/nginx/html ./deploy.sh
# On BaoTa/BT Panel, the script defaults to deploying the backend jar and
# restarting it with java -jar. Override BACKEND_RUN_MODE/DEPLOY_BACKEND_JAR
# when the backend is managed by another process manager.

APP_DIR="${APP_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)}"

BAOTA_MODE="${BAOTA_MODE:-auto}" # auto, true, or false
if [[ "${BAOTA_MODE}" == "true" || ( "${BAOTA_MODE}" == "auto" && -d /www/server/panel ) ]]; then
  IS_BAOTA_MODE=true
else
  IS_BAOTA_MODE=false
fi

if [[ "${IS_BAOTA_MODE}" == "true" ]]; then
  DEFAULT_BACKEND_RUN_MODE="jar"
  DEFAULT_DEPLOY_BACKEND_JAR="true"
  DEFAULT_HEALTHCHECK_URL="http://127.0.0.1:26904/api/health"
  DEFAULT_MAVEN_CLEAN="false"
else
  DEFAULT_BACKEND_RUN_MODE="systemd"
  DEFAULT_DEPLOY_BACKEND_JAR="true"
  DEFAULT_HEALTHCHECK_URL="http://127.0.0.1:26904/api/health"
  DEFAULT_MAVEN_CLEAN="true"
fi

BACKEND_DEPLOY_DIR="${BACKEND_DEPLOY_DIR:-/opt/javacoder/backend}"
BACKEND_JAR_NAME="${BACKEND_JAR_NAME:-javacoder-backend.jar}"
BACKEND_SERVICE="${BACKEND_SERVICE:-javacoder-backend}"
BACKEND_PORT="${BACKEND_PORT:-26904}"
BACKEND_RUN_MODE="${BACKEND_RUN_MODE:-${DEFAULT_BACKEND_RUN_MODE}}" # systemd, jar, or none
BACKEND_JAVA_OPTS="${BACKEND_JAVA_OPTS:-}"
BACKEND_PID_FILE="${BACKEND_PID_FILE:-/opt/javacoder/backend/javacoder-backend.pid}"
BACKEND_LOG_FILE="${BACKEND_LOG_FILE:-/opt/javacoder/backend/javacoder-backend.log}"
JAVACODER_SQLITE_PATH="${JAVACODER_SQLITE_PATH:-/opt/javacoder/data/javacoder.sqlite}"
DEPLOY_BACKEND_JAR="${DEPLOY_BACKEND_JAR:-${DEFAULT_DEPLOY_BACKEND_JAR}}"
BACKEND_STOP_PORT_PROCESS="${BACKEND_STOP_PORT_PROCESS:-auto}" # auto, true, or false
MAVEN_CLEAN="${MAVEN_CLEAN:-${DEFAULT_MAVEN_CLEAN}}"

FRONTEND_DEPLOY_DIR="${FRONTEND_DEPLOY_DIR:-${APP_DIR}/frontend/dist}"
FRONTEND_BUILD_DIR="${FRONTEND_BUILD_DIR:-${APP_DIR}/.deploy/frontend-dist}"

BUILD_SANDBOX_IMAGE="${BUILD_SANDBOX_IMAGE:-true}"
SANDBOX_IMAGE_NAME="${SANDBOX_IMAGE_NAME:-javacoder-java17-sandbox:latest}"

SKIP_TESTS="${SKIP_TESTS:-true}"
RUN_NPM_CI="${RUN_NPM_CI:-true}"
HEALTHCHECK_URL="${HEALTHCHECK_URL:-${DEFAULT_HEALTHCHECK_URL}}"
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
  local mvn_goals=(package)

  log "Building frontend"
  npm --prefix "${APP_DIR}/frontend" run build -- --outDir "${FRONTEND_BUILD_DIR}" --emptyOutDir

  log "Building backend"
  if [[ "${MAVEN_CLEAN}" == "true" ]]; then
    mvn_goals=(clean package)
  fi

  if [[ "${SKIP_TESTS}" == "true" ]]; then
    mvn -f "${APP_DIR}/backend/pom.xml" "${mvn_goals[@]}" -DskipTests
  else
    mvn -f "${APP_DIR}/backend/pom.xml" "${mvn_goals[@]}"
  fi

  if [[ "${BUILD_SANDBOX_IMAGE}" == "true" ]]; then
    log "Building Java sandbox Docker image: ${SANDBOX_IMAGE_NAME}"
    run_sudo docker build -f "${APP_DIR}/backend/Dockerfile.sandbox-java17" -t "${SANDBOX_IMAGE_NAME}" "${APP_DIR}/backend"
  fi
}

deploy_frontend() {
  log "Deploying frontend to ${FRONTEND_DEPLOY_DIR}"
  [[ -d "${FRONTEND_BUILD_DIR}" ]] || fail "Frontend build output not found: ${FRONTEND_BUILD_DIR}"

  run_sudo mkdir -p "${FRONTEND_DEPLOY_DIR}"
  run_sudo rsync -a --delete --exclude='.user.ini' "${FRONTEND_BUILD_DIR}/" "${FRONTEND_DEPLOY_DIR}/"
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

backend_port_pids() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -ti "tcp:${BACKEND_PORT}" -sTCP:LISTEN 2>/dev/null || true
  elif command -v fuser >/dev/null 2>&1; then
    fuser "${BACKEND_PORT}/tcp" 2>/dev/null || true
  elif command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :${BACKEND_PORT}" 2>/dev/null \
      | sed -nE 's/.*pid=([0-9]+),.*/\1/p' \
      | sort -u
  fi
}

stop_backend_port_processes() {
  if [[ "${BACKEND_STOP_PORT_PROCESS}" == "false" ]]; then
    return 0
  fi
  if [[ "${BACKEND_STOP_PORT_PROCESS}" == "auto" && "${BACKEND_RUN_MODE}" != "jar" ]]; then
    return 0
  fi

  local pids
  pids="$(backend_port_pids | tr '\n' ' ' | xargs || true)"
  if [[ -z "${pids}" ]]; then
    return 0
  fi

  log "Stopping process(es) listening on backend port ${BACKEND_PORT}: ${pids}"
  for pid in ${pids}; do
    if [[ "${pid}" =~ ^[0-9]+$ ]]; then
      run_sudo kill "${pid}" || true
    fi
  done
  sleep 3

  local remaining
  remaining="$(backend_port_pids | tr '\n' ' ' | xargs || true)"
  [[ -z "${remaining}" ]] || fail "Backend port ${BACKEND_PORT} is still in use by process(es): ${remaining}"
}

restart_backend_jar() {
  local target_jar
  target_jar="${BACKEND_DEPLOY_DIR}/${BACKEND_JAR_NAME}"

  log "Restarting backend with java -jar"
  run_sudo mkdir -p "$(dirname "${BACKEND_PID_FILE}")" "$(dirname "${BACKEND_LOG_FILE}")"
  run_sudo mkdir -p "$(dirname "${JAVACODER_SQLITE_PATH}")"

  if run_sudo test -f "${BACKEND_PID_FILE}"; then
    local old_pid
    old_pid="$(run_sudo cat "${BACKEND_PID_FILE}" || true)"
    if [[ -n "${old_pid}" ]] && run_sudo kill -0 "${old_pid}" >/dev/null 2>&1; then
      run_sudo kill "${old_pid}" || true
      sleep 3
    fi
  fi

  stop_backend_port_processes

  if [[ "${USE_SUDO}" == "true" || ( "${USE_SUDO}" == "auto" && "${EUID}" -ne 0 ) ]]; then
    sudo sh -c "JAVACODER_SQLITE_PATH='${JAVACODER_SQLITE_PATH}' nohup java ${BACKEND_JAVA_OPTS} -jar '${target_jar}' >> '${BACKEND_LOG_FILE}' 2>&1 & echo \$! > '${BACKEND_PID_FILE}'"
  else
    sh -c "JAVACODER_SQLITE_PATH='${JAVACODER_SQLITE_PATH}' nohup java ${BACKEND_JAVA_OPTS} -jar '${target_jar}' >> '${BACKEND_LOG_FILE}' 2>&1 & echo \$! > '${BACKEND_PID_FILE}'"
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
  if [[ "${IS_BAOTA_MODE}" == "true" ]]; then
    log "BaoTa/BT Panel mode enabled: backend jar deployment and java -jar restart are enabled by default"
  fi
  check_prerequisites
  install_dependencies
  build_project
  deploy_frontend
  if [[ "${DEPLOY_BACKEND_JAR}" == "true" ]]; then
    deploy_backend_jar
  else
    log "Skipping backend jar deployment because DEPLOY_BACKEND_JAR=false"
  fi
  restart_backend
  wait_for_healthcheck
  log "Deployment completed"
}

main "$@"
