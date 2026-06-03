<template>
  <main class="app-shell">
    <section v-if="viewMode === 'home'" class="home-view">
      <header class="home-header">
        <div>
          <p class="eyebrow">JavaCoder Online Judge</p>
          <h1>选择一道题，进入代码编辑界面</h1>
          <p class="home-subtitle">从题库开始练习 Java 17，提交后立即查看编译与用例结果。</p>
        </div>
        <div class="home-side">
          <div class="account-bar">
            <template v-if="currentUser">
              <span class="avatar-mark">{{ currentUser.username.slice(0, 1).toUpperCase() }}</span>
              <div>
                <p>当前用户</p>
                <strong>{{ currentUser.username }}</strong>
              </div>
              <button class="ghost-button compact-button" type="button" @click="logout">退出</button>
            </template>
            <template v-else>
              <div>
                <p>登录后提交代码</p>
                <strong>保存你的练习记录</strong>
              </div>
              <button class="primary-button compact-button" type="button" @click="openAuthDialog('login')">登录</button>
              <button class="ghost-button compact-button" type="button" @click="openAuthDialog('register')">注册</button>
            </template>
          </div>
          <div class="home-stats" aria-label="题库统计">
            <div>
              <strong>{{ problems.length }}</strong>
              <span>题目</span>
            </div>
            <div>
              <strong>{{ acceptedProblemCount }}</strong>
              <span>已通过</span>
            </div>
            <div>
              <strong>{{ submissions.length }}</strong>
              <span>提交</span>
            </div>
          </div>
        </div>
      </header>

      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>
      <div v-if="loadingProblems" class="loading-panel">正在加载题目...</div>

      <div class="home-layout">
        <section class="problem-catalog" aria-label="题目列表">
          <button
            v-for="problem in problems"
            :key="problem.id"
            class="problem-card"
            type="button"
            @click="openProblem(problem.id)"
          >
            <span class="problem-index">No. {{ problem.id }}</span>
            <span :class="['difficulty', difficultyClass(problem.difficulty)]">
              {{ displayDifficulty(problem.difficulty) }}
            </span>
            <strong>{{ problem.title }}</strong>
            <span class="tag-row compact">
              <span v-for="tag in problem.tags" :key="tag">{{ tag }}</span>
            </span>
            <span class="problem-foot">
              <span>通过 {{ problem.acceptedCount }}/{{ problem.submissionCount }}</span>
              <span>开始作答</span>
            </span>
          </button>
        </section>

        <aside class="activity-panel">
          <div class="section-heading">
            <span>最近提交</span>
            <strong>{{ submissions.length }}</strong>
          </div>
          <div v-if="submissions.length === 0" class="empty-state">暂无提交记录</div>
          <article v-for="submission in submissions.slice(0, 8)" :key="submission.id" class="submission-row">
            <div>
              <strong>{{ submission.problemTitle }}</strong>
              <p>{{ formatTime(submission.submittedAt) }}</p>
            </div>
            <span :class="['status-pill', statusClass(submission.status)]">
              {{ displayStatus(submission.status) }}
            </span>
          </article>
        </aside>
      </div>
    </section>

    <section v-else class="editor-view">
      <header class="editor-header">
        <button class="ghost-button icon-button" type="button" @click="goHome" aria-label="返回主页">
          ←
        </button>
        <div>
          <p class="eyebrow">题目 {{ selectedProblem?.id }}</p>
          <h1>{{ selectedProblem?.title || '正在加载题目' }}</h1>
        </div>
        <div class="editor-actions">
          <span v-if="selectedProblem" :class="['difficulty large', difficultyClass(selectedProblem.difficulty)]">
            {{ displayDifficulty(selectedProblem.difficulty) }}
          </span>
          <div class="account-bar compact">
            <template v-if="currentUser">
              <span class="avatar-mark">{{ currentUser.username.slice(0, 1).toUpperCase() }}</span>
              <strong>{{ currentUser.username }}</strong>
              <button class="ghost-button compact-button" type="button" @click="logout">退出</button>
            </template>
            <template v-else>
              <button class="primary-button compact-button" type="button" @click="openAuthDialog('login')">登录</button>
              <button class="ghost-button compact-button" type="button" @click="openAuthDialog('register')">注册</button>
            </template>
          </div>
        </div>
      </header>

      <div v-if="loadingProblem" class="loading-panel">正在加载题目...</div>
      <div v-if="errorMessage" class="error-banner">{{ errorMessage }}</div>

      <div v-if="selectedProblem" class="workbench">
        <section class="problem-pane">
          <div class="tag-row">
            <span v-for="tag in selectedProblem.tags" :key="tag">{{ tag }}</span>
          </div>

          <article class="statement">
            <p>{{ selectedProblem.description }}</p>
            <h2>输入</h2>
            <p>{{ selectedProblem.inputFormat }}</p>
            <h2>输出</h2>
            <p>{{ selectedProblem.outputFormat }}</p>
            <h2>数据范围</h2>
            <p>{{ selectedProblem.constraints }}</p>
          </article>

          <section class="examples">
            <article v-for="(example, index) in selectedProblem.examples" :key="index" class="example-card">
              <div class="example-title">示例 {{ index + 1 }}</div>
              <pre>输入:
{{ example.input }}输出:
{{ example.output }}</pre>
              <p>{{ example.explanation }}</p>
            </article>
          </section>
        </section>

        <section class="judge-pane">
          <div class="editor-toolbar">
            <div>
              <p class="eyebrow">语言</p>
              <strong>Java 17</strong>
            </div>
            <div class="toolbar-actions">
              <button class="ghost-button" type="button" :disabled="!currentSolution" @click="openSolutionDialog">
                标准答案
              </button>
              <button class="ghost-button" type="button" :disabled="!selectedProblem" @click="resetCode">重置</button>
              <button class="primary-button" type="button" :disabled="submitting || !selectedProblem" @click="submitCode">
                {{ submitting ? '评测中...' : currentUser ? '提交' : '登录后提交' }}
              </button>
            </div>
          </div>

          <textarea
            v-model="code"
            spellcheck="false"
            class="code-editor"
            aria-label="Java 源代码编辑器"
          />

          <section v-if="latestResult" class="result-panel">
            <div class="result-summary">
              <div>
                <p class="eyebrow">评测结果</p>
                <h2 :class="statusClass(latestResult.status)">{{ displayStatus(latestResult.status) }}</h2>
              </div>
              <div class="score-box">
                <strong>{{ latestResult.passedCases }}/{{ latestResult.totalCases }}</strong>
                <span>{{ latestResult.runtimeMs }} 毫秒</span>
              </div>
            </div>
            <p class="result-message">{{ latestResult.message }}</p>

            <div class="case-list">
              <article v-for="caseResult in latestResult.caseResults" :key="caseResult.caseNumber" class="case-card">
                <div class="case-head">
                  <strong>用例 {{ caseResult.caseNumber }}</strong>
                  <span :class="['status-pill', statusClass(caseResult.status)]">
                    {{ displayStatus(caseResult.status) }}
                  </span>
                </div>
                <template v-if="!caseResult.hidden">
                  <pre>输入: {{ caseResult.input }}
期望输出: {{ caseResult.expectedOutput }}
实际输出: {{ caseResult.actualOutput }}</pre>
                </template>
                <p v-else>隐藏测试用例</p>
              </article>
            </div>
          </section>
        </section>
      </div>
    </section>

    <div v-if="showSolutionDialog" class="modal-backdrop" role="presentation" @click.self="closeSolutionDialog">
      <section class="solution-dialog" role="dialog" aria-modal="true" aria-labelledby="solution-title">
        <header class="solution-header">
          <div>
            <p class="eyebrow">参考代码</p>
            <h2 id="solution-title">{{ selectedProblem?.title }} 标准答案</h2>
          </div>
          <button class="ghost-button icon-button" type="button" aria-label="关闭标准答案" @click="closeSolutionDialog">
            ×
          </button>
        </header>
        <pre class="solution-code">{{ currentSolution }}</pre>
        <footer class="solution-actions">
          <button class="ghost-button" type="button" @click="copySolutionCode">
            {{ copiedSolution ? '已复制' : '复制代码' }}
          </button>
          <button class="primary-button" type="button" @click="closeSolutionDialog">关闭</button>
        </footer>
      </section>
    </div>

    <div v-if="showAuthDialog" class="modal-backdrop" role="presentation" @click.self="closeAuthDialog">
      <section class="auth-dialog" role="dialog" aria-modal="true" aria-labelledby="auth-title">
        <header class="auth-header">
          <div>
            <p class="eyebrow">Account</p>
            <h2 id="auth-title">{{ authMode === 'login' ? '登录 JavaCoder' : '注册新账号' }}</h2>
          </div>
          <button class="ghost-button icon-button" type="button" aria-label="关闭登录注册窗口" @click="closeAuthDialog">
            ×
          </button>
        </header>

        <div class="auth-tabs" role="tablist" aria-label="登录注册切换">
          <button
            :class="{ active: authMode === 'login' }"
            type="button"
            role="tab"
            :aria-selected="authMode === 'login'"
            @click="switchAuthMode('login')"
          >
            登录
          </button>
          <button
            :class="{ active: authMode === 'register' }"
            type="button"
            role="tab"
            :aria-selected="authMode === 'register'"
            @click="switchAuthMode('register')"
          >
            注册
          </button>
        </div>

        <form class="auth-form" @submit.prevent="submitAuth">
          <label>
            <span>用户名</span>
            <input v-model.trim="authForm.username" autocomplete="username" maxlength="20" required />
          </label>
          <label>
            <span>密码</span>
            <input
              v-model="authForm.password"
              :autocomplete="authMode === 'login' ? 'current-password' : 'new-password'"
              minlength="6"
              maxlength="64"
              type="password"
              required
            />
          </label>
          <div v-if="authMessage" class="auth-message">{{ authMessage }}</div>
          <button class="primary-button" type="submit" :disabled="authenticating">
            {{ authenticating ? '处理中...' : authMode === 'login' ? '登录' : '创建账号' }}
          </button>
        </form>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const viewMode = ref('home')
const problems = ref([])
const selectedProblem = ref(null)
const submissions = ref([])
const code = ref('')
const latestResult = ref(null)
const loadingProblems = ref(false)
const loadingProblem = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const showSolutionDialog = ref(false)
const copiedSolution = ref(false)
const currentUser = ref(null)
const authToken = ref(localStorage.getItem('javacoder-auth-token') || '')
const showAuthDialog = ref(false)
const authMode = ref('login')
const authForm = ref({
  username: '',
  password: ''
})
const authMessage = ref('')
const authenticating = ref(false)
const pendingAuthAction = ref(null)
let submissionPollTimer = null

const activeProblemId = computed(() => selectedProblem.value?.id)
const acceptedProblemCount = computed(() => problems.value.filter((problem) => problem.acceptedCount > 0).length)
const currentSolution = computed(() => solutionCodeByProblemId[activeProblemId.value] || '')

const solutionCodeByProblemId = {
  1: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int target = scanner.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = scanner.nextInt();
        }

        Map<Integer, Integer> seen = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int need = target - nums[i];
            if (seen.containsKey(need)) {
                System.out.println(seen.get(need) + " " + i);
                return;
            }
            seen.put(nums[i], i);
        }
    }
}`,
  2: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String s = scanner.nextLine();
        Deque<Character> stack = new ArrayDeque<>();

        for (char ch : s.toCharArray()) {
            if (ch == '(' || ch == '[' || ch == '{') {
                stack.push(ch);
            } else {
                if (stack.isEmpty()) {
                    System.out.println(false);
                    return;
                }

                char left = stack.pop();
                if ((ch == ')' && left != '(')
                        || (ch == ']' && left != '[')
                        || (ch == '}' && left != '{')) {
                    System.out.println(false);
                    return;
                }
            }
        }

        System.out.println(stack.isEmpty());
    }
}`,
  3: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        int[] nums = new int[n];
        for (int i = 0; i < n; i++) {
            nums[i] = scanner.nextInt();
        }

        int[] tails = new int[n];
        int length = 0;
        for (int num : nums) {
            int left = 0;
            int right = length;
            while (left < right) {
                int mid = left + (right - left) / 2;
                if (tails[mid] < num) {
                    left = mid + 1;
                } else {
                    right = mid;
                }
            }
            tails[left] = num;
            if (left == length) {
                length++;
            }
        }

        System.out.println(length);
    }
}`
}

onMounted(async () => {
  await restoreSession()
  await Promise.all([loadProblems(), loadSubmissions()])

  const hashProblemId = parseProblemIdFromHash()
  if (hashProblemId) {
    await openProblem(hashProblemId, false)
  }

  window.addEventListener('hashchange', syncViewFromHash)
  window.addEventListener('popstate', syncViewFromHash)
})

async function loadProblems() {
  loadingProblems.value = true
  errorMessage.value = ''
  try {
    problems.value = await requestJson('/api/problems')
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loadingProblems.value = false
  }
}

async function loadSubmissions() {
  try {
    submissions.value = await requestJson('/api/submissions')
  } catch (error) {
    errorMessage.value = error.message
  }
}

async function restoreSession() {
  if (!authToken.value) {
    return
  }

  try {
    currentUser.value = await requestJson('/api/auth/me', {
      headers: authHeaders()
    })
  } catch (error) {
    authToken.value = ''
    currentUser.value = null
    localStorage.removeItem('javacoder-auth-token')
  }
}

async function openProblem(problemId, updateHash = true) {
  viewMode.value = 'editor'
  loadingProblem.value = true
  errorMessage.value = ''
  latestResult.value = null
  selectedProblem.value = null
  showSolutionDialog.value = false
  copiedSolution.value = false

  if (updateHash) {
    const targetHash = `#problem/${problemId}`
    if (window.location.hash !== targetHash) {
      window.history.pushState(null, '', targetHash)
    }
  }

  try {
    selectedProblem.value = await requestJson(`/api/problems/${problemId}`)
    code.value = selectedProblem.value.starterCode
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loadingProblem.value = false
  }
}

function goHome() {
  viewMode.value = 'home'
  selectedProblem.value = null
  latestResult.value = null
  errorMessage.value = ''
  showSolutionDialog.value = false
  copiedSolution.value = false
  window.history.pushState(null, '', `${window.location.pathname}${window.location.search}`)
}

async function syncViewFromHash() {
  const hashProblemId = parseProblemIdFromHash()
  if (hashProblemId) {
    await openProblem(hashProblemId, false)
  } else {
    viewMode.value = 'home'
  }
}

function parseProblemIdFromHash() {
  const match = window.location.hash.match(/^#?\/?problem\/(\d+)$/)
  return match ? Number(match[1]) : null
}

function resetCode() {
  if (selectedProblem.value) {
    code.value = selectedProblem.value.starterCode
  }
}

function openSolutionDialog() {
  if (!currentSolution.value) {
    return
  }

  copiedSolution.value = false
  showSolutionDialog.value = true
}

function closeSolutionDialog() {
  showSolutionDialog.value = false
}

async function copySolutionCode() {
  if (!currentSolution.value) {
    return
  }

  try {
    await navigator.clipboard.writeText(currentSolution.value)
    copiedSolution.value = true
  } catch (error) {
    errorMessage.value = '复制失败，请手动选中代码复制。'
  }
}

async function submitCode() {
  if (!activeProblemId.value) {
    return
  }

  if (!requireLogin(() => submitCode(), '请先登录，登录后即可提交代码。')) {
    return
  }

  submitting.value = true
  errorMessage.value = ''
  try {
    latestResult.value = await requestJson('/api/submissions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify({
        problemId: activeProblemId.value,
        language: 'java',
        code: code.value
      })
    })
    await Promise.all([loadProblems(), loadSubmissions()])
    await pollSubmission(latestResult.value.id)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    submitting.value = false
  }
}

async function pollSubmission(submissionId) {
  clearSubmissionPoll()
  for (let attempt = 0; attempt < 80; attempt++) {
    const result = await requestJson(`/api/submissions/${submissionId}`)
    latestResult.value = result
    await loadSubmissions()

    if (!isPendingStatus(result.status)) {
      await loadProblems()
      return
    }

    await delay(1000)
  }
  errorMessage.value = '评测仍在进行中，请稍后刷新提交记录查看结果。'
}

function clearSubmissionPoll() {
  if (submissionPollTimer) {
    clearTimeout(submissionPollTimer)
    submissionPollTimer = null
  }
}

function delay(ms) {
  return new Promise((resolve) => {
    submissionPollTimer = setTimeout(resolve, ms)
  })
}

function isPendingStatus(status) {
  return ['Pending', 'Judging'].includes(status)
}

function openAuthDialog(mode = 'login') {
  authMode.value = mode
  authMessage.value = ''
  showAuthDialog.value = true
}

function closeAuthDialog() {
  showAuthDialog.value = false
  authMessage.value = ''
  authForm.value.password = ''
  pendingAuthAction.value = null
}

function switchAuthMode(mode) {
  authMode.value = mode
  authMessage.value = ''
  authForm.value.password = ''
}

async function submitAuth() {
  authenticating.value = true
  authMessage.value = ''
  errorMessage.value = ''
  let actionToRun = null

  try {
    const authResult = await requestJson(`/api/auth/${authMode.value}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(authForm.value)
    })

    authToken.value = authResult.token
    currentUser.value = authResult.user
    localStorage.setItem('javacoder-auth-token', authResult.token)
    actionToRun = pendingAuthAction.value
    pendingAuthAction.value = null
    closeAuthDialog()
  } catch (error) {
    authMessage.value = error.message
  } finally {
    authenticating.value = false
  }

  if (actionToRun) {
    await actionToRun()
  }
}

async function logout() {
  const token = authToken.value
  authToken.value = ''
  currentUser.value = null
  pendingAuthAction.value = null
  localStorage.removeItem('javacoder-auth-token')
  authForm.value.password = ''

  if (token) {
    await requestJson('/api/auth/logout', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` }
    }).catch(() => null)
  }
}

function authHeaders() {
  return authToken.value ? { Authorization: `Bearer ${authToken.value}` } : {}
}

function requireLogin(action, message = '请先登录后再继续。') {
  if (currentUser.value) {
    return true
  }

  pendingAuthAction.value = action
  openAuthDialog('login')
  authMessage.value = message
  return false
}

async function requestJson(url, options) {
  const response = await fetch(url, options)
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.message || '请求失败，请确认后端服务正在运行。')
  }
  return data
}

function difficultyClass(difficulty) {
  return {
    easy: ['Easy', '简单'].includes(difficulty),
    medium: ['Medium', '中等'].includes(difficulty),
    hard: ['Hard', '困难'].includes(difficulty)
  }
}

function statusClass(status) {
  return {
    accepted: ['Accepted', '通过'].includes(status),
    wrong: ['Wrong Answer', '答案错误'].includes(status),
    error: ['Compile Error', 'Runtime Error', 'Judge Error', 'Unsupported Language', 'Source Limit Exceeded', 'Memory Limit Exceeded', 'Output Limit Exceeded', '编译错误', '运行错误', '评测错误', '不支持的语言', '源码超限', '内存超限', '输出超限'].includes(status),
    timeout: ['Time Limit Exceeded', 'Compile Timeout', '运行超时', '编译超时'].includes(status),
    pending: ['Pending', 'Judging', '等待评测', '评测中'].includes(status)
  }
}

function displayDifficulty(difficulty) {
  return {
    Easy: '简单',
    Medium: '中等',
    Hard: '困难'
  }[difficulty] || difficulty
}

function displayStatus(status) {
  return {
    Accepted: '通过',
    'Wrong Answer': '答案错误',
    'Compile Error': '编译错误',
    'Runtime Error': '运行错误',
    'Judge Error': '评测错误',
    'Unsupported Language': '不支持的语言',
    'Time Limit Exceeded': '运行超时',
    'Compile Timeout': '编译超时',
    'Source Limit Exceeded': '源码超限',
    'Memory Limit Exceeded': '内存超限',
    'Output Limit Exceeded': '输出超限',
    Pending: '等待评测',
    Judging: '评测中'
  }[status] || status
}

function formatTime(value) {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(new Date(value))
}
</script>

<style scoped>
:global(*) {
  box-sizing: border-box;
}

:global(body) {
  margin: 0;
  font-family: "Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", sans-serif;
  background:
    linear-gradient(135deg, rgba(37, 71, 98, 0.13), transparent 35%),
    linear-gradient(45deg, rgba(197, 87, 64, 0.12), transparent 30%),
    #f5f1e8;
  color: #202326;
}

button,
textarea,
input {
  font: inherit;
}

button {
  border: 0;
}

.app-shell {
  min-height: 100vh;
}

.home-view,
.editor-view {
  min-height: 100vh;
  padding: 34px;
}

.home-header,
.editor-header,
.home-layout,
.workbench,
.editor-toolbar,
.result-summary,
.case-head,
.section-heading,
.problem-foot {
  display: flex;
  gap: 16px;
}

.home-header,
.editor-header,
.section-heading,
.problem-foot,
.result-summary,
.case-head,
.editor-toolbar {
  align-items: center;
  justify-content: space-between;
}

.home-header,
.editor-header {
  margin: 0 auto 28px;
  max-width: 1360px;
}

.home-header h1,
.editor-header h1,
.result-summary h2 {
  margin: 0;
  letter-spacing: 0;
}

.home-header h1 {
  max-width: 820px;
  font-size: clamp(34px, 5vw, 68px);
  line-height: 1.02;
}

.editor-header h1 {
  font-size: clamp(28px, 4vw, 44px);
}

.home-subtitle {
  max-width: 580px;
  margin: 16px 0 0;
  color: #5c625f;
  font-size: 17px;
  line-height: 1.7;
}

.eyebrow {
  margin: 0 0 8px;
  color: #a94f2f;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.home-stats {
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.58);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  overflow: hidden;
}

.home-side {
  width: min(420px, 100%);
  display: grid;
  gap: 14px;
}

.account-bar {
  min-height: 72px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 253, 247, 0.78);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.account-bar > div {
  flex: 1;
  min-width: 0;
}

.account-bar p,
.account-bar strong {
  margin: 0;
}

.account-bar p {
  color: #6f7774;
  font-size: 12px;
  font-weight: 900;
}

.account-bar strong {
  color: #202326;
}

.account-bar.compact {
  min-height: 44px;
  padding: 7px;
  background: rgba(255, 253, 247, 0.64);
}

.avatar-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #202326;
  color: #fff8ea;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  font-weight: 900;
}

.home-stats div {
  min-height: 94px;
  padding: 18px;
  display: grid;
  place-items: center;
  border-left: 1px solid rgba(32, 35, 38, 0.1);
}

.home-stats div:first-child {
  border-left: 0;
}

.home-stats strong {
  font-size: 30px;
}

.home-stats span {
  color: #606966;
  font-weight: 800;
}

.home-layout {
  align-items: flex-start;
  max-width: 1360px;
  margin: 0 auto;
}

.problem-catalog {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr));
  gap: 18px;
}

.problem-card {
  min-height: 246px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 20px;
  background: #fffdf7;
  color: #202326;
  cursor: pointer;
  display: grid;
  grid-template-rows: auto auto 1fr auto auto;
  gap: 14px;
  text-align: left;
  box-shadow: 0 20px 48px rgba(64, 55, 41, 0.08);
  transition: transform 160ms ease, border-color 160ms ease, box-shadow 160ms ease;
}

.problem-card:hover {
  transform: translateY(-3px);
  border-color: #a94f2f;
  box-shadow: 0 26px 58px rgba(64, 55, 41, 0.14);
}

.problem-card strong {
  font-size: 24px;
  line-height: 1.25;
}

.problem-index {
  color: #6f7774;
  font-size: 13px;
  font-weight: 900;
}

.difficulty,
.status-pill {
  width: fit-content;
  min-height: 26px;
  border-radius: 999px;
  padding: 4px 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #e7e0d2;
  color: #202326;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.difficulty.large {
  min-height: 34px;
  font-size: 13px;
}

.easy,
.accepted {
  background: #d9efdf;
  color: #1e6b3c;
}

.medium,
.timeout,
.pending {
  background: #ffe2a8;
  color: #84500f;
}

.hard,
.wrong,
.error {
  background: #ffd9d4;
  color: #972a1f;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-row span {
  border: 1px solid #d6cabb;
  border-radius: 999px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  font-weight: 800;
}

.tag-row.compact span {
  padding: 5px 9px;
  font-size: 12px;
}

.problem-foot {
  color: #59625f;
  font-size: 13px;
  font-weight: 900;
}

.activity-panel {
  width: 340px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 20px;
  background: #202326;
  color: #fff8ea;
}

.section-heading {
  margin-bottom: 12px;
  color: rgba(255, 248, 234, 0.72);
  font-size: 13px;
  font-weight: 900;
}

.submission-row {
  border-top: 1px solid rgba(255, 248, 234, 0.12);
  padding: 12px 0;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
}

.submission-row strong,
.submission-row p {
  margin: 0;
}

.submission-row p,
.empty-state {
  color: rgba(255, 248, 234, 0.58);
  font-size: 12px;
}

.editor-view {
  background:
    linear-gradient(90deg, rgba(255, 253, 247, 0.82), rgba(255, 253, 247, 0.52)),
    #f5f1e8;
}

.editor-header {
  border-bottom: 1px solid rgba(32, 35, 38, 0.12);
  padding-bottom: 18px;
}

.editor-header > div {
  flex: 1;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workbench {
  align-items: stretch;
  max-width: 1360px;
  margin: 0 auto;
}

.problem-pane,
.judge-pane,
.result-panel,
.loading-panel,
.error-banner,
.example-card,
.case-card,
.solution-dialog {
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  background: rgba(255, 253, 247, 0.74);
}

.problem-pane {
  flex: 0 0 43%;
  max-height: calc(100vh - 164px);
  padding: 24px;
  overflow-y: auto;
}

.judge-pane {
  flex: 1;
  padding: 20px;
  display: grid;
  grid-template-rows: auto minmax(420px, 1fr) auto;
  gap: 16px;
  background: rgba(255, 255, 255, 0.78);
}

.statement {
  margin-top: 20px;
}

.statement p,
.example-card p,
.result-message,
.case-card p {
  line-height: 1.72;
}

.statement h2 {
  margin: 24px 0 8px;
  font-size: 17px;
}

.examples {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 26px;
}

.example-card,
.case-card,
.result-panel,
.loading-panel,
.error-banner {
  padding: 16px;
}

.example-title {
  margin-bottom: 10px;
  font-weight: 900;
}

pre,
.code-editor {
  font-family: "Cascadia Code", "JetBrains Mono", Consolas, monospace;
}

pre {
  margin: 0;
  color: #2c3435;
  line-height: 1.65;
  white-space: pre-wrap;
}

.editor-toolbar {
  min-height: 48px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
}

.ghost-button,
.primary-button {
  min-height: 38px;
  border-radius: 8px;
  padding: 0 15px;
  font-weight: 900;
  cursor: pointer;
}

.compact-button {
  min-height: 34px;
  padding: 0 12px;
  white-space: nowrap;
}

.ghost-button {
  border: 1px solid #b9ad9d;
  background: transparent;
  color: #202326;
}

.icon-button {
  width: 42px;
  padding: 0;
  font-size: 21px;
}

.primary-button {
  border: 1px solid #202326;
  background: #202326;
  color: #fff8ea;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.code-editor {
  width: 100%;
  min-height: 420px;
  resize: vertical;
  border: 1px solid #2b3334;
  border-radius: 8px;
  padding: 18px;
  background: #101718;
  color: #f6f0e2;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  tab-size: 4;
}

.code-editor:focus {
  border-color: #a94f2f;
  box-shadow: 0 0 0 3px rgba(169, 79, 47, 0.18);
}

.result-summary h2 {
  font-size: 28px;
}

.score-box {
  min-width: 96px;
  min-height: 64px;
  border-radius: 8px;
  padding: 8px;
  background: #202326;
  color: #fff8ea;
  display: grid;
  place-items: center;
}

.score-box strong,
.score-box span {
  display: block;
}

.score-box span {
  color: rgba(255, 248, 234, 0.72);
  font-size: 12px;
}

.case-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  padding: 28px;
  background: rgba(16, 23, 24, 0.62);
  display: grid;
  place-items: center;
}

.solution-dialog {
  width: min(920px, 100%);
  max-height: min(760px, calc(100vh - 56px));
  padding: 20px;
  background: #fffdf7;
  box-shadow: 0 34px 90px rgba(8, 10, 10, 0.34);
  display: grid;
  grid-template-rows: auto minmax(260px, 1fr) auto;
  gap: 16px;
}

.auth-dialog {
  width: min(430px, 100%);
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 20px;
  background: #fffdf7;
  box-shadow: 0 34px 90px rgba(8, 10, 10, 0.34);
  display: grid;
  gap: 16px;
}

.solution-header,
.solution-actions,
.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.solution-header h2,
.auth-header h2 {
  margin: 0;
  font-size: 24px;
  letter-spacing: 0;
}

.auth-tabs {
  border: 1px solid #d6cabb;
  border-radius: 8px;
  padding: 4px;
  background: #f3eadc;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
}

.auth-tabs button {
  min-height: 36px;
  border-radius: 6px;
  background: transparent;
  color: #59625f;
  cursor: pointer;
  font-weight: 900;
}

.auth-tabs button.active {
  background: #202326;
  color: #fff8ea;
}

.auth-form {
  display: grid;
  gap: 14px;
}

.auth-form label {
  display: grid;
  gap: 7px;
  color: #59625f;
  font-size: 13px;
  font-weight: 900;
}

.auth-form input {
  width: 100%;
  min-height: 44px;
  border: 1px solid #b9ad9d;
  border-radius: 8px;
  padding: 0 12px;
  background: #fffaf0;
  color: #202326;
  outline: none;
}

.auth-form input:focus {
  border-color: #a94f2f;
  box-shadow: 0 0 0 3px rgba(169, 79, 47, 0.18);
}

.auth-message {
  border: 1px solid #e6a79e;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff1ef;
  color: #972a1f;
  font-weight: 900;
  line-height: 1.5;
}

.solution-code {
  min-height: 260px;
  max-height: 520px;
  margin: 0;
  border: 1px solid #2b3334;
  border-radius: 8px;
  padding: 18px;
  overflow: auto;
  background: #101718;
  color: #f6f0e2;
  font-size: 14px;
}

.solution-actions {
  justify-content: flex-end;
}

.error-banner {
  max-width: 1360px;
  margin: 0 auto 16px;
  border-color: #e6a79e;
  background: #fff1ef;
  color: #972a1f;
  font-weight: 900;
}

.loading-panel {
  max-width: 1360px;
  margin: 0 auto 16px;
  font-weight: 900;
}

@media (max-width: 1120px) {
  .home-header,
  .home-layout,
  .workbench {
    flex-direction: column;
  }

  .home-stats,
  .home-side,
  .activity-panel,
  .problem-pane,
  .judge-pane {
    width: 100%;
    flex-basis: auto;
  }

  .problem-catalog {
    width: 100%;
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .problem-pane {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .home-view,
  .editor-view {
    padding: 20px;
  }

  .home-header,
  .editor-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .editor-actions {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
  }

  .account-bar {
    width: 100%;
    flex-wrap: wrap;
    justify-content: flex-start;
  }

  .home-stats,
  .problem-catalog,
  .examples {
    grid-template-columns: 1fr;
  }

  .home-stats div {
    min-height: 72px;
    border-left: 0;
    border-top: 1px solid rgba(32, 35, 38, 0.1);
  }

  .home-stats div:first-child {
    border-top: 0;
  }

  .editor-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions,
  .primary-button,
  .ghost-button {
    width: 100%;
  }

  .primary-button,
  .ghost-button {
    justify-content: center;
  }

  .modal-backdrop {
    padding: 14px;
  }

  .solution-dialog {
    max-height: calc(100vh - 28px);
  }

  .solution-header,
  .solution-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .solution-code {
    font-size: 12px;
  }
}
</style>
