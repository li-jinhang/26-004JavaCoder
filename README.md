# JavaCoder

JavaCoder 是一个参考 LeetCode 形态的在线代码评判平台原型，基于 `Spring Boot + Vue` 开发。当前版本提供题库、题目详情、Java 在线编辑、代码提交、判题结果、隐藏用例和最近提交记录等核心功能。

## 技术栈

- 后端：Java 17、Spring Boot 3.3.x、Maven
- 前端：Vue 3、Vite 5、Node.js/npm
- 默认端口：后端 `26904`，前端 `26004`

## 已实现功能

- 内置 3 道算法题示例
- 提供题目列表、题目详情、提交记录和健康检查接口
- 支持 Java 17 代码提交
- 后端调用本机 `javac` 和 `java` 进行演示级判题
- 支持 `Accepted`、`Wrong Answer`、`Compile Error`、`Runtime Error`、`Time Limit Exceeded` 等结果
- 隐藏测试用例参与判题，但不会向前端返回输入和期望输出
- 前端提供题单、题面、示例、代码编辑区、判题反馈和最近提交记录

## 项目结构

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
|  |- package.json
|  |- vite.config.js
|  |- index.html
|  `- src/
|     |- main.js
|     `- App.vue
|- .gitignore
|- .nvmrc
`- README.md
```

## 环境要求

启动前请确认本机已安装：

```bash
java -version
mvn -version
node -v
npm -v
```

根目录提供跨 Windows/Linux 的 npm 编排脚本。首次使用前，在项目根目录安装根级开发依赖：

```bash
npm install
```

如果前端依赖还未安装，可以在根目录执行：

```bash
npm run install:all
```

## 本地启动

推荐在根目录一键启动前后端：

```bash
npm run dev
```

启动后：

- 前端：`http://localhost:26004`
- 后端：`http://localhost:26904`
- 健康检查：`http://localhost:26904/api/health`

前端已通过 `vite.config.js` 配置代理，开发时访问 `/api/*` 会自动转发到 `http://localhost:26904`。

也可以分开启动两个项目。

启动后端：

```bash
cd backend
mvn spring-boot:run
```

后端地址：

- `http://localhost:26904`
- 健康检查：`http://localhost:26904/api/health`

启动前端：

```bash
cd frontend
npm install
npm run dev
```

前端地址：

- `http://localhost:26004`

## 接口概览

- `GET /api/health`：健康检查
- `GET /api/problems`：获取题目列表
- `GET /api/problems/{id}`：获取题目详情
- `GET /api/submissions`：获取最近提交记录
- `POST /api/submissions`：提交代码并判题

提交示例：

```json
{
  "problemId": 1,
  "language": "java",
  "code": "public class Main { public static void main(String[] args) { } }"
}
```

## 打包构建

推荐在根目录一键构建前后端：

```bash
npm run build
```

构建产物：

- 前端：`frontend/dist/`
- 后端：`backend/target/javacoder-backend-0.0.1-SNAPSHOT.jar`

也可以分开构建两个项目。

后端：

```bash
cd backend
mvn clean package
```

前端：

```bash
cd frontend
npm run build
```

## 部署调试

部署端建议按两个项目拆分部署，便于分别看日志、重启和升级。

后端单独部署：

```bash
cd backend
mvn clean package
java -jar target/javacoder-backend-0.0.1-SNAPSHOT.jar
```

前端单独部署：

```bash
cd frontend
npm ci
npm run build
```

将 `frontend/dist/` 发布到 Nginx 或其他静态文件服务器，并把 `/api/` 反向代理到后端 `26904` 端口。示例：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /var/www/javacoder/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:26904/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 安全说明

当前判题模块适合本地演示、课程作业或原型验证。它会在宿主机上编译并执行用户提交的 Java 代码，虽然设置了进程超时，但不是生产级沙箱。

如果要用于真实线上环境，应将判题服务迁移到隔离 worker，并结合容器或 microVM、CPU/内存限制、文件系统限制、网络限制、任务队列和审计日志。

## 后续建议

- 接入 MySQL 或 PostgreSQL 持久化题目、用户和提交记录
- 增加登录注册、权限控制和题目管理后台
- 将判题逻辑拆分为独立 worker 服务
- 增加 Docker Compose 一键启动环境
- 补充后端单元测试和前后端集成测试
