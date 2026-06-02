# JavaCoder 项目上下文

## 项目定位

JavaCoder 是一个本地演示级在线代码评测平台原型，形态接近简化版 LeetCode。项目由 Spring Boot 后端和 Vue/Vite 前端组成，当前核心能力包括：

- 展示内置算法题列表和题目详情
- 在浏览器里编辑 Java 17 代码
- 提交代码到后端编译、运行并判题
- 展示判题结论、通过用例数、运行时间和可见用例详情
- 保存进程内最近提交记录

当前项目主要适合本地演示、课程作业或原型验证。判题逻辑会在宿主机上直接执行用户提交的 Java 代码，虽然有超时控制，但不是生产级沙箱。

## 技术栈

后端：

- Java 17
- Spring Boot 3.3.5
- Maven 项目，配置文件为 `backend/pom.xml`
- 服务端口：`26904`

前端：

- Vue 3，单文件组件
- Vite 5
- npm
- 开发端口：`26004`
- 前端 `/api` 请求由 Vite 代理到 `http://localhost:26904`

根目录：

- `package.json` 只作为开发编排层使用，不改变前后端独立项目边界。
- `npm run dev` 同时启动前端 Vite 和后端 Spring Boot。
- `npm run build` 先构建前端，再构建后端。

## 启动与构建

推荐从根目录一键启动：

```bash
npm install
npm run install:all
npm run dev
```

启动后：

- 前端：`http://localhost:26004`
- 后端：`http://localhost:26904`
- 健康检查：`http://localhost:26904/api/health`

根目录一键构建：

```bash
npm run build
```

构建产物：

- 前端：`frontend/dist/`
- 后端：`backend/target/javacoder-backend-0.0.1-SNAPSHOT.jar`

也可以按两个项目分开启动和构建。

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

前端构建：

```bash
cd frontend
npm run build
```

注意：当前仓库没有 Maven Wrapper。如果本机没有安装 `mvn`，后端构建命令无法执行。

## 部署边界

开发端可以在根目录用 npm 一键编排前后端；部署调试时仍按前端、后端两个项目拆分。

后端部署：

```bash
cd backend
mvn clean package
java -jar target/javacoder-backend-0.0.1-SNAPSHOT.jar
```

前端部署：

```bash
cd frontend
npm ci
npm run build
```

部署端推荐将 `frontend/dist/` 放到 Nginx 或其他静态文件服务器，并把 `/api/` 反向代理到后端服务。这样前端代码仍然只请求 `/api/*`，本地开发由 Vite 代理，线上由 Nginx 代理。

## 目录结构

```text
26-004JavaCoder/
|- package.json
|- backend/
|  |- pom.xml
|  `- src/main/
|     |- java/com/example/javacoder/
|     |  |- controller/
|     |  |- model/
|     |  |- repository/
|     |  |- service/
|     |  `- JavaCoderApplication.java
|     `- resources/application.yml
|- frontend/
|  |- index.html
|  |- package.json
|  |- vite.config.js
|  `- src/
|     |- App.vue
|     `- main.js
|- README.md
`- CONTEXT.md
```

## 后端架构

入口：

- `backend/src/main/java/com/example/javacoder/JavaCoderApplication.java`
  Spring Boot 应用入口。

控制器：

- `controller/HealthController.java`
  提供 `GET /api/health` 健康检查。

- `controller/ProblemController.java`
  提供题目列表和题目详情：
  - `GET /api/problems`
  - `GET /api/problems/{id}`

- `controller/SubmissionController.java`
  提供提交记录和代码提交：
  - `GET /api/submissions`
  - `POST /api/submissions`
  提交时先根据 `problemId` 找题目，再调用 `JavaJudgeService` 判题，并把结果写入 `SubmissionStore`。

题库：

- `repository/ProblemRepository.java`
  当前使用内存中的 `Map<Long, Problem>` 存放题目，没有数据库。内置 3 道题：
  - 两数之和
  - 有效的括号
  - 最长递增子序列

服务：

- `service/JavaJudgeService.java`
  判题核心服务。流程是：
  1. 校验语言只支持 `java`
  2. 校验代码非空
  3. 创建临时目录
  4. 写入 `Main.java`
  5. 执行 `javac Main.java`
  6. 对每个测试用例执行 `java -cp <workDir> Main`
  7. 标准化输出并与期望输出比较
  8. 返回 `Submission`
  9. 删除临时目录

- `service/SubmissionStore.java`
  使用 `CopyOnWriteArrayList` 保存最近提交记录，只存在内存中。应用重启后提交记录会丢失。

## 后端数据模型

- `Problem`
  完整题目实体，包含题面、输入输出格式、约束、起始代码、示例和测试用例。

- `ProblemSummary`
  题目列表接口返回的摘要，包含标题、难度、标签、通过数和提交数。

- `ProblemDetail`
  题目详情接口返回的数据，不包含隐藏测试用例本体。

- `ExampleCase`
  前端展示用示例，包含输入、输出和解释。

- `TestCase`
  判题用例，包含输入、期望输出和是否隐藏。

- `SubmissionRequest`
  前端提交请求，字段为 `problemId`、`language`、`code`。

- `Submission`
  判题结果，包含状态、通过用例数、总用例数、运行耗时、消息和用例结果。

- `TestCaseResult`
  单个测试用例的判题结果。隐藏用例不会返回输入、期望输出和实际输出。

## 判题状态约定

后端内部状态仍使用英文稳定值，前端负责显示成中文。这些英文值不要随意改名，否则会影响统计和样式映射：

- `Accepted`
- `Wrong Answer`
- `Compile Error`
- `Runtime Error`
- `Judge Error`
- `Unsupported Language`
- `Time Limit Exceeded`
- `Compile Timeout`

其中 `SubmissionStore.acceptedCountByProblemId` 通过字符串 `Accepted` 统计通过数。前端 `App.vue` 的 `statusClass` 和 `displayStatus` 也依赖这些值。

难度字段目前后端已使用中文：

- `简单`
- `中等`
- `困难`

前端仍兼容旧英文难度值 `Easy`、`Medium`、`Hard`。

## 前端架构

入口：

- `frontend/src/main.js`
  创建 Vue 应用并挂载到 `#app`。

主组件：

- `frontend/src/App.vue`
  当前前端所有界面、状态管理、API 请求和样式都集中在这个单文件组件中。

页面布局分三栏：

- 左侧：品牌、题目列表、最近提交
- 中间：题目详情、标签、题面、示例
- 右侧：Java 编辑区、提交按钮、判题结果

主要状态：

- `problems`：题目列表
- `selectedProblem`：当前题目详情
- `submissions`：最近提交记录
- `code`：编辑器内容
- `latestResult`：最近一次提交结果
- `loadingProblem`：题目加载状态
- `submitting`：提交状态
- `errorMessage`：接口错误信息

主要方法：

- `loadProblems()`：加载题目列表
- `loadSubmissions()`：加载最近提交
- `selectProblem(problemId)`：加载题目详情，并填充起始代码
- `resetCode()`：恢复当前题目的起始代码
- `submitCode()`：提交代码并刷新题目列表、最近提交
- `requestJson()`：统一发起请求并处理错误
- `difficultyClass()`：难度样式分类
- `statusClass()`：判题状态样式分类
- `displayDifficulty()`：难度显示文案
- `displayStatus()`：判题状态显示文案
- `formatTime()`：用 `zh-CN` 格式化提交时间

## 接口概览

```text
GET  /api/health
GET  /api/problems
GET  /api/problems/{id}
GET  /api/submissions
POST /api/submissions
```

提交请求示例：

```json
{
  "problemId": 1,
  "language": "java",
  "code": "public class Main { public static void main(String[] args) { } }"
}
```

## 本地化现状

网站可见内容已经中文化，包括：

- 浏览器标题
- 题目列表、最近提交、按钮、提示语
- 题面标题、输入、输出、数据范围、示例
- 判题结论、用例详情、错误提示
- 内置题目的题名、标签、题面和示例说明

为了保持判题逻辑稳定，后端状态值仍保留英文，前端显示层负责翻译。

## 重要约束与注意事项

- 根目录 `package.json` 是跨平台编排入口，便于 Windows 开发端一键启动和构建；不要把它理解为前后端合并成一个运行时。
- 用户提交代码必须包含 `public class Main`，因为后端固定写入并编译 `Main.java`，运行入口也是 `Main`。
- 当前判题会真实调用本机 `javac` 和 `java`。运行后端的机器必须安装 JDK，而不仅是 JRE。
- `JavaJudgeService` 的编译超时为 8 秒，单个用例运行超时为 3 秒。
- 隐藏用例参与判题，但不会把输入、期望输出、实际输出返回给前端。
- 目前没有数据库、用户系统、鉴权、题目管理后台或生产级沙箱。
- `frontend/dist` 是构建产物。修改前端源码后运行 `npm run build` 会更新它。
- 根目录当前不是 Git 仓库，无法依赖 `git status` 区分用户改动。

## 后续开发方向

- 增加 Maven Wrapper，降低后端启动门槛。
- 增加后端单元测试，覆盖判题服务、题目接口和提交接口。
- 增加前端组件拆分，把 `App.vue` 中的三栏 UI 拆成更小组件。
- 接入数据库，持久化题目、提交记录和用户。
- 将判题服务迁移到隔离 worker，并用容器或 microVM 做生产级沙箱。
- 增加 Docker Compose，统一启动前后端和未来数据库。
