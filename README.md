# 实验室设备管理系统 (Laboratory Equipment Management System)

## 🛠 技术栈
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia
- **Backend**: Java Spring Boot + Spring Data JPA
- **Database**: MySQL 8.0
- **DevOps**: Docker + Docker Compose

## ⚙️ 配置说明

项目使用 **环境变量 + 默认值** 的方式管理配置，避免敏感信息硬编码在源码中。所有配置项都有合理的默认值，开箱即用。

### 配置原则
- **本地开发零配置**：所有配置项均有默认值，直接运行即可
- **敏感信息外置**：数据库密码、JWT 密钥等通过环境变量注入
- **一套变量名**：Docker 环境和本地开发使用统一的环境变量命名
- **向后兼容**：不创建独立的配置文件树，统一通过环境变量覆盖

### Docker Compose 环境变量

在项目根目录创建 `.env` 文件（参考 `.env.example`），可覆盖以下配置：

| 变量名 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `MYSQL_ROOT_PASSWORD` | `3434` | MySQL root 用户密码 |
| `MYSQL_DATABASE` | `lab_db` | MySQL 数据库名 |
| `DB_PORT` | `3307` | MySQL 宿主机映射端口 |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://db:3306/lab_db?...` | Spring 数据源连接 URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | 数据库用户名 |
| `SPRING_DATASOURCE_PASSWORD` | `3434` | 数据库密码 |
| `JWT_SECRET` | `lab-equipment-...` | JWT 签名密钥（生产环境务必修改） |
| `JWT_EXPIRATION_MS` | `86400000` | JWT 过期时间（毫秒，默认 24 小时） |
| `BACKEND_PORT` | `8080` | 后端服务宿主机映射端口 |
| `FRONTEND_PORT` | `3000` | 前端服务宿主机映射端口 |

### 前端开发环境变量

在 `frontend/` 目录创建 `.env` 文件（参考 `frontend/.env.example`）：

| 变量名 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `VITE_DEV_PORT` | `3000` | Vite 开发服务器端口 |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | 后端 API 代理目标地址 |

### 后端本地开发配置

后端 Spring Boot 支持直接通过环境变量或 `application.yml` 配置。常用方式：

```bash
# 方式一：设置环境变量后运行
export SPRING_DATASOURCE_PASSWORD=your_password
export JWT_SECRET=your_secret_key
mvn spring-boot:run

# 方式二：启动时传参
mvn spring-boot:run -Dspring-boot.run.arguments="--SPRING_DATASOURCE_PASSWORD=xxx --JWT_SECRET=xxx"
```

## 🚀 How to Run

### 方式一：Docker Compose（推荐）

1. 确保 **Docker Desktop** 已安装并运行。
2. （可选）复制 `.env.example` 为 `.env`，根据需要修改配置。
3. 在项目根目录执行以下命令启动所有服务：
   ```bash
   docker compose up --build -d
   ```
4. 等待容器构建并启动完成（首次运行可能需要几分钟下载镜像）。

### 方式二：本地开发

1. 启动 MySQL（本地或 Docker 方式均可）。
2. 后端：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. 前端：
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

## Services
- **Frontend (前端页面)**: http://localhost:3000
- **Backend API (后端接口)**: http://localhost:8080

## 🧪 Verification-基本验证方式
您可以访问前端页面并使用以下默认账号登录，验证系统功能：

| 角色 (Role) | 用户名 (Username) | 密码 (Password) | 权限说明 |
| :--- | :--- | :--- | :--- |
| **管理员 (Admin)** | `admin` | `admin` | 拥有所有权限：用户管理、实验室增删改查、审批借用、维修管理等 |
| **教师 (Teacher)** | `teacher` | `123456` | 可申请借用设备、申请报修、归还设备 |
| **学生 (Student)** | `student` | `123456` | 基础查看权限 |

## 📷 功能介绍
1. **实验室管理**：支持实验室的增删改查 (CRUD)。
2. **设备管理**：设备的录入、状态查看及管理。
3. **借用管理**：
   - 教师/学生申请借用。
   - 管理员审批/拒绝。
   - 借用中/已归还状态流转。
4. **维修管理**：
   - 设备故障报修。
   - 管理员处理维修完成。
5. **用户管理**：管理员可添加、修改、删除用户及分配角色。
