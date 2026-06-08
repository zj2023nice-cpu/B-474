# 实验室设备管理系统 (Laboratory Equipment Management System)

## 🛠 技术栈
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia
- **Backend**: Java Spring Boot + Spring Data JPA
- **Database**: MySQL 8.0
- **DevOps**: Docker + Docker Compose

## 🚀 How to Run
1. 确保 **Docker Desktop** 已安装并运行。
2. 在项目根目录执行以下命令启动所有服务：
   ```bash
   docker compose up --build -d
   ```
3. 等待容器构建并启动完成（首次运行可能需要几分钟下载镜像）。

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
