<template>
  <main class="app-shell">
    <section v-if="viewMode === 'home'" class="home-view">
      <header class="home-header">
        <div>
          <p class="eyebrow">JavaCoder Online Judge</p>
          <h1>代码在线练习平台</h1>
          <p class="home-subtitle">轻松学会一门高级语言！</p>
        </div>
        <div class="home-side">
          <div class="account-bar">
            <template v-if="currentUser">
              <span class="avatar-mark">{{ currentUser.username.slice(0, 1).toUpperCase() }}</span>
              <div>
                <p>当前用户</p>
                <strong>{{ currentUser.username }}</strong>
              </div>
              <span v-if="isAdmin" class="role-badge">管理员</span>
              <button v-if="isAdmin" class="ghost-button compact-button" type="button" @click="openAdminPanel">用户管理</button>
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

      <section class="catalog-toolbar" aria-label="题目搜索与筛选">
        <label class="search-box">
          <span>搜索</span>
          <input
            v-model.trim="searchQuery"
            type="search"
            placeholder="题号、标题、标签"
            autocomplete="off"
          />
        </label>
        <div class="difficulty-filter" role="tablist" aria-label="按难度筛选">
          <button
            v-for="option in difficultyOptions"
            :key="option"
            :class="{ active: difficultyFilter === option }"
            type="button"
            role="tab"
            :aria-selected="difficultyFilter === option"
            @click="difficultyFilter = option"
          >
            {{ option }}
          </button>
        </div>
        <div class="toolbar-count">
          <strong>{{ filteredProblems.length }}</strong>
          <span>/ {{ problems.length }} 题</span>
        </div>
      </section>

      <div class="home-layout">
        <section class="problem-catalog" aria-label="题目列表">
          <button
            v-for="problem in filteredProblems"
            :key="problem.id"
            class="problem-card"
            type="button"
            @click="openProblem(problem.id)"
          >
            <span class="problem-card-top">
              <span class="problem-index">No. {{ formatProblemNumber(problem.id) }}</span>
              <span :class="['difficulty', difficultyClass(problem.difficulty)]">
                {{ displayDifficulty(problem.difficulty) }}
              </span>
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
          <div v-if="!loadingProblems && filteredProblems.length === 0" class="catalog-empty">
            <strong>没有找到匹配题目</strong>
            <span>换个关键词或清除难度筛选</span>
          </div>
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
        <div class="editor-title">
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
              <span v-if="isAdmin" class="role-badge">管理员</span>
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

      <div
        v-if="selectedProblem"
        ref="workbenchRef"
        :class="['workbench', { 'problem-collapsed': problemPaneCollapsed, resizing: isResizingProblemPane }]"
        :style="workbenchStyle"
      >
        <section :class="['problem-pane', { collapsed: problemPaneCollapsed }]">
          <button
            v-if="problemPaneCollapsed"
            class="problem-restore-button"
            type="button"
            aria-label="展开题目描述"
            @click="problemPaneCollapsed = false"
          >
            <span aria-hidden="true">&gt;</span>
            <strong>题目描述</strong>
          </button>

          <template v-else>
            <header class="problem-module-head">
              <div>
                <p class="eyebrow">Problem</p>
                <strong>题目描述</strong>
              </div>
              <button
                class="ghost-button icon-button pane-collapse-button"
                type="button"
                aria-label="折叠题目描述"
                @click="problemPaneCollapsed = true"
              >
                &lt;
              </button>
            </header>

            <div class="problem-tabs" role="tablist" aria-label="题目内容切换">
              <button
                :class="{ active: activeProblemTab === 'statement' }"
                type="button"
                role="tab"
                :aria-selected="activeProblemTab === 'statement'"
                @click="activeProblemTab = 'statement'"
              >
                题目
              </button>
              <button
                :class="{ active: activeProblemTab === 'solutions' }"
                type="button"
                role="tab"
                :aria-selected="activeProblemTab === 'solutions'"
                @click="activeProblemTab = 'solutions'"
              >
                题解
              </button>
            </div>

            <div v-show="activeProblemTab === 'statement'" class="tag-row">
              <span v-for="tag in selectedProblem.tags" :key="tag">{{ tag }}</span>
            </div>

            <article v-show="activeProblemTab === 'statement'" class="statement">
              <p>{{ selectedProblem.description }}</p>
              <h2>输入</h2>
              <p>{{ selectedProblem.inputFormat }}</p>
              <h2>输出</h2>
              <p>{{ selectedProblem.outputFormat }}</p>
              <h2>数据范围</h2>
              <p>{{ selectedProblem.constraints }}</p>
            </article>

            <section v-show="activeProblemTab === 'statement'" class="examples">
              <article v-for="(example, index) in selectedProblem.examples" :key="index" class="example-card">
                <div class="example-title">示例 {{ index + 1 }}</div>
                <pre>输入:
{{ example.input }}输出:
{{ example.output }}</pre>
                <p>{{ example.explanation }}</p>
              </article>
            </section>

            <section v-show="activeProblemTab === 'solutions'" class="solution-board">
              <header class="solution-board-head">
                <div>
                  <p class="eyebrow">Solutions</p>
                  <strong>{{ userSolutions.length }} 篇题解</strong>
                </div>
                <button class="primary-button compact-button" type="button" @click="openSolutionEditor()">
                  写题解
                </button>
              </header>

              <div v-if="solutionMessage" class="admin-message">{{ solutionMessage }}</div>
              <div v-if="loadingSolutions" class="loading-panel compact-panel">正在加载题解...</div>
              <div v-else-if="userSolutions.length === 0" class="catalog-empty solution-empty">
                <strong>暂无题解</strong>
                <span>写下思路、坑点或复杂度分析，给下一次复盘留个锚点。</span>
              </div>
              <article v-for="solution in userSolutions" :key="solution.id" class="user-solution-card">
                <header>
                  <div>
                    <span class="solution-meta">
                      {{ solution.authorName }} · {{ formatTime(solution.updatedAt) }} · {{ formatSolutionLanguage(solution.language) }}
                    </span>
                    <h3>{{ solution.title }}</h3>
                  </div>
                  <div v-if="canManageSolution(solution)" class="solution-card-actions">
                    <button
                      v-if="canEditSolution(solution)"
                      class="ghost-button compact-button"
                      type="button"
                      @click="openSolutionEditor(solution)"
                    >
                      编辑
                    </button>
                    <button class="ghost-button compact-button danger-button" type="button" @click="deleteSolution(solution)">删除</button>
                  </div>
                </header>
                <p class="solution-content">{{ solution.content }}</p>
                <pre v-if="solution.code" class="solution-snippet">{{ solution.code }}</pre>
              </article>
            </section>
          </template>
        </section>

        <div
          v-if="!problemPaneCollapsed"
          class="pane-resizer"
          role="separator"
          aria-label="调整题目和代码模块宽度"
          aria-orientation="vertical"
          @mousedown="startProblemPaneResize"
          @dblclick="resetProblemPaneWidth"
        >
          <span aria-hidden="true"></span>
        </div>

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

    <div v-if="showAdminPanel && isAdmin" class="modal-backdrop" role="presentation" @click.self="closeAdminPanel">
      <section class="admin-panel" role="dialog" aria-modal="true" aria-label="管理员用户管理">
        <header class="admin-panel-head">
          <div>
            <p class="eyebrow">Admin Console</p>
            <h2>用户管理</h2>
          </div>
          <button class="ghost-button icon-button" type="button" aria-label="关闭用户管理" @click="closeAdminPanel">
            ×
          </button>
        </header>

        <form class="admin-search" @submit.prevent="loadAdminUsers">
          <label class="search-box">
            <span>用户</span>
            <input
              v-model.trim="adminSearchQuery"
              type="search"
              placeholder="搜索用户 ID 或名称"
              autocomplete="off"
            />
          </label>
          <button class="primary-button" type="submit" :disabled="loadingAdminUsers">
            {{ loadingAdminUsers ? '搜索中...' : '搜索' }}
          </button>
        </form>

        <div v-if="adminMessage" class="admin-message">{{ adminMessage }}</div>
        <div v-if="loadingAdminUsers" class="loading-panel compact-panel">正在加载用户...</div>
        <div v-else-if="adminUsers.length === 0" class="catalog-empty admin-empty">
          <strong>没有找到用户</strong>
          <span>换个 ID 或用户名再试试</span>
        </div>
        <div v-else class="admin-user-list">
          <article v-for="user in adminUsers" :key="user.id" class="admin-user-row">
            <div class="admin-user-main">
              <span class="avatar-mark">{{ user.username.slice(0, 1).toUpperCase() }}</span>
              <div>
                <strong>{{ user.username }}</strong>
                <p>ID {{ user.id }} · {{ formatTime(user.createdAt) }}</p>
              </div>
            </div>
            <span :class="['role-badge', { admin: user.role === 'ADMIN' }]">
              {{ formatUserRole(user.role) }}
            </span>
            <button
              class="ghost-button compact-button danger-button"
              type="button"
              :disabled="user.currentUser || deletingUserId === user.id"
              @click="deleteAdminUser(user)"
            >
              {{ deletingUserId === user.id ? '删除中...' : user.currentUser ? '当前账号' : '删除' }}
            </button>
          </article>
        </div>
      </section>
    </div>

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

    <div v-if="showSolutionEditorDialog" class="modal-backdrop" role="presentation" @click.self="closeSolutionEditor">
      <section class="user-solution-dialog" role="dialog" aria-modal="true" aria-labelledby="user-solution-title">
        <header class="solution-header">
          <div>
            <p class="eyebrow">User Solution</p>
            <h2 id="user-solution-title">{{ editingSolution ? '编辑题解' : '发布题解' }}</h2>
          </div>
          <button class="ghost-button icon-button" type="button" aria-label="关闭题解编辑窗口" @click="closeSolutionEditor">
            ×
          </button>
        </header>

        <form class="user-solution-form" @submit.prevent="submitSolution">
          <label>
            <span>标题</span>
            <input v-model.trim="solutionForm.title" maxlength="80" required />
          </label>
          <label>
            <span>正文</span>
            <textarea
              v-model.trim="solutionForm.content"
              class="solution-textarea"
              maxlength="20000"
              required
            />
          </label>
          <label>
            <span>语言</span>
            <input v-model.trim="solutionForm.language" maxlength="32" />
          </label>
          <label>
            <span>代码片段</span>
            <textarea v-model="solutionForm.code" class="solution-code-input" spellcheck="false" />
          </label>
          <div v-if="solutionMessage" class="auth-message">{{ solutionMessage }}</div>
          <footer class="solution-actions">
            <button class="ghost-button" type="button" @click="closeSolutionEditor">取消</button>
            <button class="primary-button" type="submit" :disabled="savingSolution">
              {{ savingSolution ? '保存中...' : editingSolution ? '保存修改' : '发布题解' }}
            </button>
          </footer>
        </form>
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
import { computed, onMounted, onUnmounted, ref } from 'vue'

const viewMode = ref('home')
const problems = ref([])
const selectedProblem = ref(null)
const problemPaneCollapsed = ref(false)
const problemPaneWidth = ref(42)
const isResizingProblemPane = ref(false)
const workbenchRef = ref(null)
const submissions = ref([])
const code = ref('')
const latestResult = ref(null)
const searchQuery = ref('')
const difficultyFilter = ref('全部')
const loadingProblems = ref(false)
const loadingProblem = ref(false)
const submitting = ref(false)
const errorMessage = ref('')
const showSolutionDialog = ref(false)
const copiedSolution = ref(false)
const activeProblemTab = ref('statement')
const userSolutions = ref([])
const loadingSolutions = ref(false)
const solutionMessage = ref('')
const showSolutionEditorDialog = ref(false)
const savingSolution = ref(false)
const editingSolution = ref(null)
const solutionForm = ref({
  title: '',
  content: '',
  language: 'java',
  code: ''
})
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
const showAdminPanel = ref(false)
const adminSearchQuery = ref('')
const adminUsers = ref([])
const loadingAdminUsers = ref(false)
const adminMessage = ref('')
const deletingUserId = ref(null)
let submissionPollTimer = null

const difficultyOptions = ['全部', '简单', '中等', '困难']
const activeProblemId = computed(() => selectedProblem.value?.id)
const acceptedProblemCount = computed(() => problems.value.filter((problem) => problem.acceptedCount > 0).length)
const currentSolution = computed(() => selectedProblem.value?.referenceSolution || '')
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const workbenchStyle = computed(() => ({
  '--problem-pane-width': `${problemPaneWidth.value}%`
}))
const filteredProblems = computed(() => {
  const keyword = searchQuery.value.trim().toLowerCase()

  return problems.value.filter((problem) => {
    const difficulty = displayDifficulty(problem.difficulty)
    const matchesDifficulty = difficultyFilter.value === '全部' || difficulty === difficultyFilter.value
    if (!matchesDifficulty) {
      return false
    }

    if (!keyword) {
      return true
    }

    const searchableText = [
      problem.id,
      formatProblemNumber(problem.id),
      problem.title,
      difficulty,
      ...problem.tags
    ].join(' ').toLowerCase()

    return searchableText.includes(keyword)
  })
})


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

onUnmounted(() => {
  stopProblemPaneResize()
})

function startProblemPaneResize(event) {
  if (problemPaneCollapsed.value) {
    return
  }

  event.preventDefault()
  isResizingProblemPane.value = true
  document.body.classList.add('resizing-layout')
  window.addEventListener('mousemove', resizeProblemPane)
  window.addEventListener('mouseup', stopProblemPaneResize)
}

function resizeProblemPane(event) {
  const workbench = workbenchRef.value
  if (!workbench) {
    return
  }

  const bounds = workbench.getBoundingClientRect()
  const dividerWidth = 10
  const minProblemPane = 260
  const minJudgePane = 520
  const maxProblemPane = Math.max(minProblemPane, bounds.width - minJudgePane - dividerWidth)
  const nextWidth = Math.min(Math.max(event.clientX - bounds.left, minProblemPane), maxProblemPane)

  problemPaneWidth.value = Number(((nextWidth / bounds.width) * 100).toFixed(2))
}

function stopProblemPaneResize() {
  if (!isResizingProblemPane.value) {
    return
  }

  isResizingProblemPane.value = false
  document.body.classList.remove('resizing-layout')
  window.removeEventListener('mousemove', resizeProblemPane)
  window.removeEventListener('mouseup', stopProblemPaneResize)
}

function resetProblemPaneWidth() {
  problemPaneWidth.value = 42
}

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

async function loadSolutions(problemId = activeProblemId.value) {
  if (!problemId) {
    userSolutions.value = []
    return
  }

  loadingSolutions.value = true
  solutionMessage.value = ''
  try {
    userSolutions.value = await requestJson(`/api/problems/${problemId}/solutions`)
  } catch (error) {
    solutionMessage.value = error.message
  } finally {
    loadingSolutions.value = false
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

async function openAdminPanel() {
  if (!isAdmin.value) {
    return
  }

  showAdminPanel.value = true
  await loadAdminUsers()
}

function closeAdminPanel() {
  showAdminPanel.value = false
  adminMessage.value = ''
}

async function loadAdminUsers() {
  if (!isAdmin.value) {
    return
  }

  loadingAdminUsers.value = true
  adminMessage.value = ''
  try {
    const query = encodeURIComponent(adminSearchQuery.value.trim())
    adminUsers.value = await requestJson(`/api/admin/users?query=${query}`, {
      headers: authHeaders()
    })
  } catch (error) {
    adminMessage.value = error.message
  } finally {
    loadingAdminUsers.value = false
  }
}

async function deleteAdminUser(user) {
  if (!isAdmin.value || user.currentUser) {
    return
  }

  const confirmed = window.confirm(`确定删除用户 ${user.username}（ID ${user.id}）吗？此操作不可恢复。`)
  if (!confirmed) {
    return
  }

  deletingUserId.value = user.id
  adminMessage.value = ''
  try {
    await requestJson(`/api/admin/users/${user.id}`, {
      method: 'DELETE',
      headers: authHeaders()
    })
    adminUsers.value = adminUsers.value.filter((item) => item.id !== user.id)
    adminMessage.value = `已删除用户 ${user.username}。`
  } catch (error) {
    adminMessage.value = error.message
  } finally {
    deletingUserId.value = null
  }
}

async function openProblem(problemId, updateHash = true) {
  viewMode.value = 'editor'
  loadingProblem.value = true
  errorMessage.value = ''
  latestResult.value = null
  problemPaneCollapsed.value = false
  selectedProblem.value = null
  showSolutionDialog.value = false
  copiedSolution.value = false
  activeProblemTab.value = 'statement'
  userSolutions.value = []
  solutionMessage.value = ''
  showSolutionEditorDialog.value = false

  if (updateHash) {
    const targetHash = `#problem/${problemId}`
    if (window.location.hash !== targetHash) {
      window.history.pushState(null, '', targetHash)
    }
  }

  try {
    selectedProblem.value = await requestJson(`/api/problems/${problemId}`)
    code.value = selectedProblem.value.starterCode
    await loadSolutions(problemId)
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loadingProblem.value = false
  }
}

function goHome() {
  viewMode.value = 'home'
  selectedProblem.value = null
  problemPaneCollapsed.value = false
  latestResult.value = null
  errorMessage.value = ''
  showSolutionDialog.value = false
  copiedSolution.value = false
  activeProblemTab.value = 'statement'
  userSolutions.value = []
  solutionMessage.value = ''
  showSolutionEditorDialog.value = false
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

function openSolutionEditor(solution = null) {
  if (!requireLogin(() => openSolutionEditor(solution), '请先登录，登录后即可发布题解。')) {
    return
  }

  editingSolution.value = solution
  solutionMessage.value = ''
  solutionForm.value = solution
    ? {
        title: solution.title,
        content: solution.content,
        language: solution.language,
        code: solution.code || ''
      }
    : {
        title: '',
        content: '',
        language: 'java',
        code: ''
      }
  showSolutionEditorDialog.value = true
}

function closeSolutionEditor() {
  showSolutionEditorDialog.value = false
  savingSolution.value = false
  editingSolution.value = null
  solutionMessage.value = ''
}

async function submitSolution() {
  if (!activeProblemId.value) {
    return
  }

  if (!requireLogin(() => submitSolution(), '请先登录后再保存题解。')) {
    return
  }

  savingSolution.value = true
  solutionMessage.value = ''
  const payload = {
    title: solutionForm.value.title,
    content: solutionForm.value.content,
    language: solutionForm.value.language || 'java',
    code: solutionForm.value.code || '',
    relatedSubmissionId: null
  }

  try {
    const targetUrl = editingSolution.value
      ? `/api/solutions/${editingSolution.value.id}`
      : `/api/problems/${activeProblemId.value}/solutions`
    const savedSolution = await requestJson(targetUrl, {
      method: editingSolution.value ? 'PUT' : 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(payload)
    })

    if (editingSolution.value) {
      userSolutions.value = userSolutions.value.map((solution) =>
        solution.id === savedSolution.id ? savedSolution : solution
      )
    } else {
      userSolutions.value = [savedSolution, ...userSolutions.value]
    }
    closeSolutionEditor()
    activeProblemTab.value = 'solutions'
  } catch (error) {
    solutionMessage.value = error.message
  } finally {
    savingSolution.value = false
  }
}

async function deleteSolution(solution) {
  if (!canManageSolution(solution)) {
    return
  }

  const confirmed = window.confirm(`确定删除题解「${solution.title}」吗？`)
  if (!confirmed) {
    return
  }

  solutionMessage.value = ''
  try {
    await requestJson(`/api/solutions/${solution.id}`, {
      method: 'DELETE',
      headers: authHeaders()
    })
    userSolutions.value = userSolutions.value.filter((item) => item.id !== solution.id)
  } catch (error) {
    solutionMessage.value = error.message
  }
}

function canManageSolution(solution) {
  return Boolean(
    currentUser.value
      && (currentUser.value.role === 'ADMIN' || currentUser.value.id === solution.authorId)
  )
}

function canEditSolution(solution) {
  return Boolean(currentUser.value && currentUser.value.id === solution.authorId)
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
  let shouldOpenAdminPanel = false

  try {
    const authResult = await requestJson(`/api/auth/${authMode.value}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(authForm.value)
    })

    authToken.value = authResult.token
    currentUser.value = authResult.user
    localStorage.setItem('javacoder-auth-token', authResult.token)
    shouldOpenAdminPanel = authResult.user.role === 'ADMIN'
    actionToRun = shouldOpenAdminPanel ? null : pendingAuthAction.value
    pendingAuthAction.value = null
    closeAuthDialog()
  } catch (error) {
    authMessage.value = error.message
  } finally {
    authenticating.value = false
  }

  if (shouldOpenAdminPanel) {
    await openAdminPanel()
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
  showAdminPanel.value = false
  adminUsers.value = []
  adminMessage.value = ''
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
    Hard: '困难',
    简单: '简单',
    中等: '中等',
    困难: '困难'
  }[difficulty] || difficulty
}

function formatProblemNumber(problemId) {
  return String(problemId).padStart(2, '0')
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

function formatUserRole(role) {
  return {
    ADMIN: '管理员',
    USER: '普通用户'
  }[role] || role
}

function formatSolutionLanguage(language) {
  return {
    java: 'Java'
  }[language] || language
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

.catalog-toolbar {
  max-width: 1360px;
  margin: 0 auto 18px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 12px;
  background: rgba(255, 253, 247, 0.84);
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  box-shadow: 0 18px 46px rgba(64, 55, 41, 0.08);
}

.search-box {
  min-height: 48px;
  border: 1px solid #cfc3b2;
  border-radius: 8px;
  padding: 0 12px;
  background: #fffaf0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-box span {
  color: #a94f2f;
  font-size: 12px;
  font-weight: 900;
}

.search-box input {
  width: 100%;
  min-width: 0;
  border: 0;
  background: transparent;
  color: #202326;
  outline: none;
}

.search-box input::placeholder {
  color: #8a8071;
}

.difficulty-filter {
  min-height: 48px;
  border: 1px solid #cfc3b2;
  border-radius: 8px;
  padding: 4px;
  background: #f4ecdf;
  display: grid;
  grid-template-columns: repeat(4, minmax(58px, 1fr));
  gap: 4px;
}

.difficulty-filter button {
  border-radius: 6px;
  padding: 0 12px;
  background: transparent;
  color: #59625f;
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.difficulty-filter button.active {
  background: #202326;
  color: #fff8ea;
}

.toolbar-count {
  min-height: 48px;
  border-radius: 8px;
  padding: 0 14px;
  background: #2c3435;
  color: #fff8ea;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  white-space: nowrap;
}

.toolbar-count strong {
  font-size: 22px;
}

.toolbar-count span {
  color: rgba(255, 248, 234, 0.72);
  font-weight: 900;
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

.role-badge {
  min-height: 28px;
  border: 1px solid #d0b47c;
  border-radius: 999px;
  padding: 4px 10px;
  background: #fff0c4;
  color: #704b0c;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.role-badge.admin {
  border-color: #9b7a34;
  background: #202326;
  color: #fff8ea;
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
  grid-template-columns: repeat(4, minmax(210px, 1fr));
  gap: 14px;
}

.problem-card {
  min-height: 198px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 16px;
  background: #fffdf7;
  color: #202326;
  cursor: pointer;
  display: grid;
  grid-template-rows: auto 1fr auto auto;
  gap: 12px;
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
  font-size: 20px;
  line-height: 1.25;
}

.problem-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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
  position: sticky;
  top: 24px;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 20px;
  background: #202326;
  color: #fff8ea;
}

.admin-panel {
  width: min(920px, 100%);
  max-height: min(780px, calc(100vh - 56px));
  max-width: 1360px;
  margin: 0 auto;
  border: 1px solid rgba(32, 35, 38, 0.14);
  border-radius: 8px;
  padding: 20px;
  background: rgba(255, 253, 247, 0.9);
  box-shadow: 0 20px 54px rgba(64, 55, 41, 0.1);
  overflow: auto;
}

.admin-panel-head,
.admin-search,
.admin-user-row,
.admin-user-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-panel-head,
.admin-user-row {
  justify-content: space-between;
}

.admin-panel-head {
  margin-bottom: 16px;
}

.admin-panel-head h2 {
  margin: 0;
  font-size: 28px;
}

.admin-search {
  align-items: stretch;
  margin-bottom: 14px;
}

.admin-search .search-box {
  flex: 1;
}

.admin-message {
  border: 1px solid #d6cabb;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fffaf0;
  color: #59625f;
  font-weight: 900;
}

.compact-panel {
  margin: 0;
}

.admin-empty {
  min-height: 128px;
}

.admin-user-list {
  display: grid;
  gap: 10px;
}

.admin-user-row {
  min-height: 72px;
  border: 1px solid #e0d5c5;
  border-radius: 8px;
  padding: 12px;
  background: #fffdf7;
}

.admin-user-main {
  min-width: 0;
  flex: 1;
}

.admin-user-main div {
  min-width: 0;
}

.admin-user-main strong,
.admin-user-main p {
  margin: 0;
}

.admin-user-main p {
  margin-top: 4px;
  color: #6f7774;
  font-size: 12px;
  font-weight: 900;
}

.danger-button {
  border-color: #c78980;
  color: #972a1f;
}

.catalog-empty {
  min-height: 198px;
  border: 1px dashed #b9ad9d;
  border-radius: 8px;
  padding: 24px;
  background: rgba(255, 253, 247, 0.62);
  color: #59625f;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
}

.catalog-empty strong {
  color: #202326;
  font-size: 18px;
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

  .catalog-toolbar {
    grid-template-columns: 1fr;
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

  .activity-panel {
    position: static;
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

  .admin-search,
  .admin-user-row {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-user-main {
    width: 100%;
  }

  .home-stats,
  .problem-catalog,
  .examples {
    grid-template-columns: 1fr;
  }

  .difficulty-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-count {
    justify-content: flex-start;
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

/* Visual refresh: keep the warm paper + ink palette, but make the app feel more composed. */
:global(:root) {
  --ink: #202326;
  --ink-soft: #2c3435;
  --paper: #fffdf7;
  --paper-warm: #fff8ea;
  --sand: #f5f1e8;
  --sand-deep: #e7ded0;
  --line: rgba(32, 35, 38, 0.13);
  --line-strong: rgba(32, 35, 38, 0.2);
  --rust: #a94f2f;
  --rust-soft: rgba(169, 79, 47, 0.13);
  --muted: #5c625f;
  --shadow-soft: 0 18px 44px rgba(64, 55, 41, 0.1);
  --shadow-lift: 0 28px 70px rgba(64, 55, 41, 0.16);
}

:global(body) {
  background:
    linear-gradient(135deg, rgba(37, 71, 98, 0.12), transparent 34%),
    linear-gradient(45deg, var(--rust-soft), transparent 32%),
    repeating-linear-gradient(0deg, rgba(32, 35, 38, 0.025) 0 1px, transparent 1px 28px),
    var(--sand);
  color: var(--ink);
}

.home-view,
.editor-view {
  padding: 30px;
}

.home-view {
  position: relative;
}

.home-view::before,
.editor-view::before {
  content: "";
  position: fixed;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background:
    linear-gradient(90deg, rgba(255, 253, 247, 0.72), transparent 26%, rgba(255, 253, 247, 0.52)),
    linear-gradient(180deg, rgba(255, 255, 255, 0.5), transparent 42%);
}

.home-header,
.editor-header,
.catalog-toolbar,
.home-layout,
.workbench,
.error-banner,
.loading-panel {
  max-width: 1440px;
}

.home-header {
  min-height: 266px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 28px;
  background:
    linear-gradient(110deg, rgba(255, 253, 247, 0.94) 0%, rgba(255, 248, 234, 0.78) 58%, rgba(231, 222, 208, 0.74) 100%);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.home-header > div:first-child {
  position: relative;
  z-index: 1;
}

.home-header > div:first-child::after {
  content: "";
  display: block;
  width: 94px;
  height: 5px;
  margin-top: 22px;
  border-radius: 999px;
  background: var(--rust);
}

.home-header h1 {
  max-width: 760px;
  font-size: 54px;
  line-height: 1.06;
}

.editor-header h1 {
  font-size: 36px;
  line-height: 1.16;
}

.home-subtitle {
  max-width: 650px;
  margin-top: 18px;
  color: var(--muted);
  font-size: 16px;
}

.eyebrow {
  color: var(--rust);
}

.home-side {
  width: min(450px, 100%);
  align-self: stretch;
  align-content: end;
}

.account-bar,
.home-stats,
.catalog-toolbar,
.problem-card,
.admin-panel,
.admin-user-row,
.problem-pane,
.judge-pane,
.result-panel,
.loading-panel,
.error-banner,
.example-card,
.case-card,
.solution-dialog,
.auth-dialog {
  border-color: var(--line);
  box-shadow: var(--shadow-soft);
}

.account-bar {
  background: rgba(255, 253, 247, 0.86);
  backdrop-filter: blur(12px);
}

.home-stats {
  background: var(--ink);
  color: var(--paper-warm);
}

.home-stats div {
  min-height: 104px;
  border-left-color: rgba(255, 248, 234, 0.12);
}

.home-stats strong {
  font-size: 34px;
  line-height: 1;
}

.home-stats span {
  color: rgba(255, 248, 234, 0.66);
  font-size: 12px;
  letter-spacing: 0;
}

.catalog-toolbar {
  position: sticky;
  top: 14px;
  z-index: 10;
  margin-bottom: 16px;
  border-color: rgba(32, 35, 38, 0.16);
  padding: 10px;
  background: rgba(255, 253, 247, 0.9);
  backdrop-filter: blur(14px);
}

.search-box {
  border-color: #d2c3b1;
  background: #fffaf0;
  transition: border-color 160ms ease, box-shadow 160ms ease, background 160ms ease;
}

.search-box span {
  flex: 0 0 auto;
  white-space: nowrap;
}

.search-box:focus-within {
  border-color: var(--rust);
  background: #fffdf7;
  box-shadow: 0 0 0 3px rgba(169, 79, 47, 0.14);
}

.difficulty-filter {
  border-color: #d2c3b1;
  background: #eee4d6;
}

.difficulty-filter button,
.auth-tabs button {
  transition: background 160ms ease, color 160ms ease, transform 160ms ease;
}

.difficulty-filter button:hover,
.auth-tabs button:hover {
  background: rgba(32, 35, 38, 0.07);
}

.difficulty-filter button.active,
.auth-tabs button.active {
  background: var(--ink);
  color: var(--paper-warm);
}

.toolbar-count {
  background: var(--ink);
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 342px;
  gap: 18px;
}

.problem-catalog {
  grid-template-columns: repeat(3, minmax(230px, 1fr));
  gap: 16px;
}

.problem-card {
  min-height: 214px;
  position: relative;
  border-color: rgba(32, 35, 38, 0.12);
  padding: 18px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.98), rgba(255, 248, 234, 0.82)),
    var(--paper);
  overflow: hidden;
}

.problem-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: #d6cabb;
  transition: background 160ms ease;
}

.problem-card:hover {
  transform: translateY(-4px);
  border-color: rgba(169, 79, 47, 0.55);
  box-shadow: var(--shadow-lift);
}

.problem-card:hover::before {
  background: var(--rust);
}

.problem-card strong {
  font-size: 19px;
  line-height: 1.32;
}

.problem-index,
.problem-foot,
.account-bar p,
.admin-user-main p {
  color: var(--muted);
}

.tag-row {
  gap: 7px;
}

.tag-row span {
  border-color: #d7caba;
  background: rgba(255, 255, 255, 0.62);
}

.difficulty,
.status-pill,
.role-badge {
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.38);
}

.activity-panel {
  width: auto;
  top: 82px;
  padding: 18px;
  background:
    linear-gradient(180deg, #242a2b, #171d1e),
    var(--ink);
  box-shadow: var(--shadow-lift);
}

.section-heading {
  margin-bottom: 14px;
}

.section-heading strong {
  min-width: 36px;
  min-height: 30px;
  border-radius: 8px;
  background: rgba(255, 248, 234, 0.1);
  display: inline-grid;
  place-items: center;
  color: var(--paper-warm);
}

.submission-row {
  border-top-color: rgba(255, 248, 234, 0.14);
}

.submission-row:first-of-type {
  border-top: 0;
}

.editor-view {
  background: transparent;
}

.editor-header {
  min-height: 92px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 16px 18px;
  background: rgba(255, 253, 247, 0.86);
  box-shadow: var(--shadow-soft);
}

.editor-header > div {
  min-width: 0;
}

.editor-actions {
  flex: 0 0 auto;
}

.workbench {
  display: grid;
  grid-template-columns: minmax(360px, 0.82fr) minmax(520px, 1.18fr);
  gap: 18px;
  align-items: stretch;
}

.problem-pane,
.judge-pane {
  flex: initial;
  min-width: 0;
  background: rgba(255, 253, 247, 0.86);
}

.problem-pane {
  max-height: calc(100vh - 148px);
  padding: 22px;
}

.judge-pane {
  grid-template-rows: auto minmax(500px, 1fr) auto;
  padding: 16px;
  background: rgba(255, 255, 255, 0.82);
}

.statement {
  margin-top: 18px;
}

.statement h2 {
  border-top: 1px solid var(--line);
  padding-top: 16px;
  color: var(--ink);
}

.examples {
  grid-template-columns: 1fr;
  gap: 12px;
}

.example-card,
.case-card,
.result-panel {
  background: rgba(255, 253, 247, 0.92);
}

.editor-toolbar {
  min-height: 54px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 12px;
}

.toolbar-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.ghost-button,
.primary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: transform 150ms ease, box-shadow 150ms ease, border-color 150ms ease, background 150ms ease;
}

.ghost-button:hover:not(:disabled),
.primary-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.ghost-button {
  border-color: #b8ad9d;
  background: rgba(255, 253, 247, 0.5);
}

.ghost-button:hover:not(:disabled) {
  border-color: var(--rust);
  background: rgba(255, 250, 240, 0.9);
}

.primary-button {
  border-color: var(--ink);
  background: var(--ink);
  box-shadow: 0 12px 26px rgba(32, 35, 38, 0.18);
}

.primary-button:hover:not(:disabled) {
  background: #111718;
  box-shadow: 0 16px 34px rgba(32, 35, 38, 0.24);
}

.code-editor,
.solution-code {
  border-color: #111718;
  background:
    linear-gradient(90deg, rgba(255, 248, 234, 0.04) 0 1px, transparent 1px 48px),
    linear-gradient(180deg, #101718, #0c1112);
  box-shadow: inset 0 0 0 1px rgba(255, 248, 234, 0.04);
}

.code-editor {
  min-height: 500px;
}

.result-panel {
  border-color: rgba(32, 35, 38, 0.16);
}

.score-box {
  background: var(--ink);
}

.modal-backdrop {
  background:
    linear-gradient(180deg, rgba(16, 23, 24, 0.72), rgba(16, 23, 24, 0.58)),
    rgba(16, 23, 24, 0.62);
}

.admin-panel,
.solution-dialog,
.auth-dialog {
  background: rgba(255, 253, 247, 0.96);
}

.auth-tabs {
  background: #eee4d6;
}

@media (min-width: 1500px) {
  .problem-catalog {
    grid-template-columns: repeat(4, minmax(220px, 1fr));
  }
}

@media (max-width: 1180px) {
  .home-header {
    min-height: auto;
    align-items: stretch;
    flex-direction: column;
  }

  .home-layout,
  .workbench {
    display: flex;
    flex-direction: column;
  }

  .catalog-toolbar {
    position: static;
    grid-template-columns: 1fr;
  }

  .problem-catalog {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }

  .activity-panel {
    position: static;
  }

  .problem-pane {
    max-height: none;
  }
}

@media (max-width: 760px) {
  .home-view,
  .editor-view {
    padding: 16px;
  }

  .home-header {
    padding: 20px;
  }

  .home-header h1 {
    font-size: 34px;
  }

  .editor-header h1 {
    font-size: 28px;
  }

  .home-subtitle {
    font-size: 15px;
  }

  .home-stats div {
    min-height: 82px;
    border-top: 0;
    border-left: 1px solid rgba(255, 248, 234, 0.12);
  }

  .home-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .home-stats div:first-child {
    border-left: 0;
  }

  .home-stats strong {
    font-size: 28px;
  }

  .catalog-toolbar {
    padding: 8px;
  }

  .problem-catalog,
  .examples {
    grid-template-columns: 1fr;
  }

  .problem-card {
    min-height: 188px;
  }

  .editor-header,
  .editor-actions,
  .editor-toolbar,
  .toolbar-actions,
  .solution-header,
  .solution-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .editor-actions,
  .account-bar.compact {
    width: 100%;
  }

  .judge-pane {
    grid-template-rows: auto minmax(420px, 1fr) auto;
    padding: 14px;
  }

  .code-editor {
    min-height: 420px;
  }

  .modal-backdrop {
    padding: 12px;
  }
}

.editor-header > .editor-title {
  flex: 1 1 auto;
  min-width: 0;
}

.editor-header > .editor-actions {
  flex: 0 0 auto;
  margin-left: auto;
  justify-content: flex-end;
}

.problem-module-head {
  position: sticky;
  top: -22px;
  z-index: 2;
  min-height: 58px;
  margin: -22px -22px 18px;
  border-bottom: 1px solid var(--line);
  padding: 10px 12px 10px 18px;
  background: rgba(255, 253, 247, 0.94);
  backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.problem-module-head .eyebrow,
.problem-module-head strong {
  margin: 0;
}

.problem-module-head strong {
  font-size: 17px;
}

.problem-tabs {
  margin-bottom: 16px;
  border: 1px solid #d2c3b1;
  border-radius: 8px;
  padding: 4px;
  background: #eee4d6;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px;
}

.problem-tabs button {
  min-height: 34px;
  border-radius: 6px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  font-weight: 900;
  transition: background 160ms ease, color 160ms ease;
}

.problem-tabs button.active {
  background: var(--ink);
  color: var(--paper-warm);
}

.solution-board {
  display: grid;
  gap: 12px;
}

.solution-board-head,
.user-solution-card header,
.solution-card-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.solution-board-head,
.user-solution-card header {
  justify-content: space-between;
}

.solution-board-head {
  min-height: 48px;
}

.solution-board-head strong {
  display: block;
  margin-top: 2px;
}

.solution-empty {
  min-height: 168px;
}

.user-solution-card {
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 16px;
  background: rgba(255, 253, 247, 0.94);
  box-shadow: var(--shadow-soft);
}

.user-solution-card h3 {
  margin: 4px 0 0;
  font-size: 18px;
  line-height: 1.35;
}

.solution-meta {
  color: var(--muted);
  font-size: 12px;
  font-weight: 900;
}

.solution-content {
  margin: 12px 0 0;
  color: var(--ink-soft);
  line-height: 1.75;
  white-space: pre-wrap;
}

.solution-snippet {
  max-height: 280px;
  margin-top: 12px;
  border: 1px solid #111718;
  border-radius: 8px;
  padding: 14px;
  overflow: auto;
  background: #101718;
  color: #f6f0e2;
  font-size: 13px;
}

.user-solution-dialog {
  width: min(760px, 100%);
  max-height: min(820px, calc(100vh - 56px));
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 20px;
  overflow: auto;
  background: rgba(255, 253, 247, 0.96);
  box-shadow: 0 34px 90px rgba(8, 10, 10, 0.34);
  display: grid;
  gap: 16px;
}

.user-solution-form {
  display: grid;
  gap: 14px;
}

.user-solution-form label {
  display: grid;
  gap: 7px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 900;
}

.user-solution-form input,
.solution-textarea,
.solution-code-input {
  width: 100%;
  border: 1px solid #b9ad9d;
  border-radius: 8px;
  padding: 12px;
  background: #fffaf0;
  color: var(--ink);
  outline: none;
}

.user-solution-form input {
  min-height: 44px;
}

.solution-textarea {
  min-height: 180px;
  resize: vertical;
  line-height: 1.7;
}

.solution-code-input {
  min-height: 180px;
  resize: vertical;
  font-family: "Cascadia Code", "JetBrains Mono", Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.user-solution-form input:focus,
.solution-textarea:focus,
.solution-code-input:focus {
  border-color: var(--rust);
  box-shadow: 0 0 0 3px rgba(169, 79, 47, 0.18);
}

.pane-collapse-button {
  width: 38px;
  min-height: 36px;
  font-size: 20px;
  line-height: 1;
}

.pane-resizer {
  width: 14px;
  min-width: 14px;
  cursor: col-resize;
  background: linear-gradient(90deg, transparent 0 6px, rgba(32, 35, 38, 0.1) 6px 8px, transparent 8px 100%);
  display: grid;
  place-items: center;
  touch-action: none;
  transition: background 160ms ease;
}

.pane-resizer span {
  width: 4px;
  height: 74px;
  border-radius: 999px;
  background: rgba(32, 35, 38, 0.16);
  transition: background 160ms ease, height 160ms ease;
}

.pane-resizer:hover span,
.workbench.resizing .pane-resizer span {
  height: 112px;
  background: var(--rust);
}

.pane-resizer:hover,
.workbench.resizing .pane-resizer {
  background: linear-gradient(90deg, transparent 0 6px, rgba(169, 79, 47, 0.2) 6px 8px, transparent 8px 100%);
}

.problem-pane.collapsed {
  width: 96px;
  min-width: 96px;
  padding: 0;
  overflow: hidden;
}

.problem-restore-button {
  width: 100%;
  height: 100%;
  min-height: 420px;
  border: 0;
  border-radius: 8px;
  padding: 18px 10px;
  background:
    linear-gradient(180deg, rgba(255, 253, 247, 0.98), rgba(255, 248, 234, 0.84)),
    var(--paper);
  color: var(--ink);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  font-weight: 900;
}

.problem-restore-button span {
  width: 38px;
  height: 38px;
  border: 1px solid #b8ad9d;
  border-radius: 8px;
  display: inline-grid;
  place-items: center;
  color: var(--rust);
  font-size: 22px;
  line-height: 1;
}

.problem-restore-button strong {
  writing-mode: vertical-rl;
  text-orientation: mixed;
  letter-spacing: 0;
}

.workbench.problem-collapsed {
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 14px;
}

.workbench:not(.problem-collapsed) {
  grid-template-columns: minmax(260px, var(--problem-pane-width)) 14px minmax(520px, 1fr);
  gap: 0;
}

:global(body.resizing-layout),
:global(body.resizing-layout *) {
  cursor: col-resize !important;
  user-select: none;
}

@media (max-width: 1180px) {
  .pane-resizer {
    display: none;
  }

  .problem-pane.collapsed {
    width: 100%;
    min-width: 0;
  }

  .problem-restore-button {
    min-height: 64px;
    flex-direction: row;
    justify-content: flex-start;
  }

  .problem-restore-button strong {
    writing-mode: horizontal-tb;
  }
}

@media (max-width: 760px) {
  .editor-header > .editor-actions {
    width: 100%;
    margin-left: 0;
  }

  .editor-header > .icon-button {
    width: 42px;
    min-width: 42px;
    align-self: flex-start;
  }

  .problem-module-head {
    position: static;
  }

  .solution-board-head,
  .user-solution-card header,
  .solution-card-actions {
    align-items: stretch;
    flex-direction: column;
  }

}
</style>
