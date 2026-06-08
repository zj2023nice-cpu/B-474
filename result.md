# 实验室设备管理系统 — 全栈任务分析

> 基于 fullstack-task-generator 的代码审查结果，按 Bug 修复、Feature 迭代、代码理解、代码重构、代码测试、工程化六个维度整理。

---

## Bug 修复

1. **修复密码明文存储与传输漏洞。当前 `UserService.login()` 直接拿明文做 `equals()` 比对，`DataInitializer` 和 `AuthController.createUser` 也是明文入库。请引入 `BCryptPasswordEncoder`，改造登录校验逻辑为 `matches()`，改造用户创建/更新逻辑为 `encode()`，并给存量初始化数据也加上加密。**
   - 涉及文件：`backend/src/main/java/com/example/lab/service/UserService.java`、`backend/src/main/java/com/example/lab/config/DataInitializer.java`、`backend/src/main/java/com/example/lab/controller/AuthController.java`

2. **收紧 CORS 配置，移除 `allowedOriginPatterns("*")` 与 `allowCredentials(true)` 的危险组合。当前 `WebConfig` 允许任意来源携带凭证访问后端，存在 CSRF 风险。请将 allowedOrigins 限制为前端实际部署域名（如 `http://localhost:3000`），生产环境通过环境变量注入。**
   - 涉及文件：`backend/src/main/java/com/example/lab/config/WebConfig.java`

3. **补齐后端的认证与权限拦截。目前所有 API 都是裸奔状态，前端仅靠隐藏按钮做权限控制，直接 curl 就能删用户、批借用。请引入 JWT 或 Session 拦截器，在 `EquipmentController`、`BorrowController`、`RepairController`、`UserController` 等关键端点上校验登录状态与角色（ADMIN/TEACHER/STUDENT）。**
   - 涉及文件：`backend/src/main/java/com/example/lab/controller/*`、`backend/src/main/java/com/example/lab/config/*`

4. **修复空指针异常风险。`BorrowService.apply()` 中 `borrow.getEquipment().getId()`、`EquipmentService.addEquipment()` 中 `equipment.getLab().getId()`、`RepairService.report()` 中 `repair.getEquipment().getId()` 均可能在参数缺失时抛出 NPE。请在 Service 层入口添加参数判空校验，或给 DTO 加 `@NotNull` 约束。**
   - 涉及文件：`backend/src/main/java/com/example/lab/service/BorrowService.java`、`backend/src/main/java/com/example/lab/service/EquipmentService.java`、`backend/src/main/java/com/example/lab/service/RepairService.java`

5. **修复设备编码在并发场景下的重复问题。`EquipmentService.addEquipment()` 用 `findByLab_Id().size() + 1` 生成 `LABxx-xxx` 编码，并发请求会得到相同编码并触发数据库唯一约束异常。请改用数据库自增序列、分布式 ID 或加乐观锁/悲观锁来保证编码唯一。**
   - 涉及文件：`backend/src/main/java/com/example/lab/service/EquipmentService.java`

6. **修复取消报修后设备状态卡在 REPAIRING 的问题。`RepairService` 没有实现 cancel 业务方法，前端 `RepairList.vue` 中取消报修直接调 `DELETE /repairs/{id}`，导致设备状态无法恢复为 NORMAL。请在 RepairService 中增加 cancel 方法，在删除维修记录前将关联设备状态回滚为 NORMAL，并开放对应的 PUT/DELETE 端点。**
   - 涉及文件：`backend/src/main/java/com/example/lab/service/RepairService.java`、`backend/src/main/java/com/example/lab/controller/RepairController.java`、`frontend/src/views/RepairList.vue`

7. **补全局异常处理，避免 RuntimeException 裸抛返回 500 错误页。当前 `BorrowService.apply()` 抛出的 `"该时间段设备已被预约"` 等异常，前端 `request.js` 拦截器无法稳定解析。请添加 `@ControllerAdvice` 全局异常处理器，统一包装为 `{code, message, data}` 的 JSON 响应，并确保前端错误提示能正常显示。**
   - 涉及文件：新增 `backend/src/main/java/com/example/lab/exception/GlobalExceptionHandler.java`

---

## Feature 迭代

1. **为所有列表接口增加分页与基础筛选。当前 `/equipments`、`/borrows`、`/repairs`、`/users` 均返回全量数据，`EquipmentList.vue` 的前端搜索也只是内存过滤。请在后端引入 Spring Data 的 `Pageable`，支持 `?page=&size=&keyword=` 参数；前端表格接入 Element Plus 的 `el-pagination`，并把搜索请求发给后端。**
   - 涉及文件：`backend/src/main/java/com/example/lab/controller/*Controller.java`、`backend/src/main/java/com/example/lab/service/*Service.java`、前端各 List.vue

2. **补齐设备报废（SCRAPPED）操作流程。`Equipment` 实体已定义 `SCRAPPED` 状态，但前后端都没有提供报废入口。请在 `EquipmentController` 增加 `PUT /equipments/{id}/scrap` 端点，在前端 `EquipmentList.vue` 的 admin 操作列增加"报废"按钮，报废后设备不可再被借用或报修。**
   - 涉及文件：`backend/src/main/java/com/example/lab/controller/EquipmentController.java`、`backend/src/main/java/com/example/lab/service/EquipmentService.java`、`frontend/src/views/EquipmentList.vue`

3. **新增后端聚合统计接口 `/api/stats`，替代 Dashboard 前端全量拉取。当前 `Dashboard.vue` 同时请求 equipments、borrows、repairs 三个全量列表做前端统计，数据量稍大就卡。请在后端写一个 StatsController，用 COUNT/SUM 等 SQL 聚合返回设备总数、当前借用数、待维修数、借用超期数；前端只调这一个接口。**
   - 涉及文件：新增 `backend/src/main/java/com/example/lab/controller/StatsController.java`、`frontend/src/views/Dashboard.vue`

4. **增加设备使用年限到期提醒。`Equipment` 有 `purchaseDate` 和 `lifeSpan`，可以推算报废日期。请在 `Dashboard.vue` 或新增"到期提醒"页面展示 30 天内即将到期的设备列表，方便管理员提前处理。**
   - 涉及文件：`backend/src/main/java/com/example/lab/entity/Equipment.java`、新增查询接口、`frontend/src/views/Dashboard.vue` 或新增页面

5. **增加维修单状态流转的"维修中"（IN_PROGRESS）中间态。当前 `RepairService.report()` 直接设为 REPORTED，`finish()` 直接跳到 FINISHED，跳过了 IN_PROGRESS。请在 RepairController 增加 `PUT /repairs/{id}/start` 端点，前端 RepairList.vue 增加"开始维修"按钮，让流程更完整。**
   - 涉及文件：`backend/src/main/java/com/example/lab/controller/RepairController.java`、`backend/src/main/java/com/example/lab/service/RepairService.java`、`frontend/src/views/RepairList.vue`

---

## 代码理解

1. **分析 `BorrowRepository.findConflicts()` 的时间范围冲突检测逻辑。当前查询用三段式 OR 条件判断时间段重叠：`b.startTime BETWEEN :start AND :end`、`b.endTime BETWEEN :start AND :end`、`:start BETWEEN b.startTime AND b.endTime`。请解释这个查询的覆盖范围与边界情况（例如 startTime == endTime、边界值是否被包含），并指出是否存在漏判或误判的场景。**
   - 涉及文件：`backend/src/main/java/com/example/lab/repository/BorrowRepository.java`

2. **梳理设备状态流转的并发风险。`BorrowService.approve()` 和 `returnEquipment()`、`RepairService.report()` 和 `finish()` 都在事务内修改 `Equipment.status`。请画出状态流转图（NORMAL -> BORROWED/REPAIRING -> NORMAL），分析在高并发下是否可能出现状态覆盖或脏写（例如同一台设备同时被借用和报修），并评估现有 `@Transactional` 的隔离级别是否足够。**
   - 涉及文件：`backend/src/main/java/com/example/lab/service/BorrowService.java`、`backend/src/main/java/com/example/lab/service/RepairService.java`

---

## 代码重构

1. **统一后端 API 响应格式。当前 Controller 返回类型混乱：有的直接返回 `List<Equipment>`，有的返回 `ResponseEntity.ok(user)`，有的返回 `ResponseEntity.status(401).body("用户名或密码错误")`。请引入统一的 `Result<T>` 包装类（含 code、message、data），改造所有 Controller 的返回值，让前端拦截器能稳定判断成功/失败。**
   - 涉及文件：`backend/src/main/java/com/example/lab/controller/*Controller.java`、新增 `backend/src/main/java/com/example/lab/common/Result.java`

2. **提取前端状态映射公共函数，消除重复代码。当前 `EquipmentList.vue`、`BorrowList.vue`、`RepairList.vue` 都各自定义了 `getStatusText()` 和 `getStatusType()`，逻辑高度重复。请把这些映射抽到 `frontend/src/utils/status.js`，并在各页面导入使用。**
   - 涉及文件：新增 `frontend/src/utils/status.js`、`frontend/src/views/EquipmentList.vue`、`frontend/src/views/BorrowList.vue`、`frontend/src/views/RepairList.vue`

3. **给后端实体 DTO 增加参数校验注解。当前所有 `@RequestBody` 都没有加 `@Valid`，仅靠数据库层的 `@Column(nullable = false)` 做最后一道防线。请在 `LoginRequest`、`User`、`Equipment`、`Borrow` 等实体的关键字段上加 `@NotBlank`、`@NotNull`、`@Positive` 等校验注解，并在 Controller 参数前加 `@Valid`，让非法请求在入口就被拦住。**
   - 涉及文件：`backend/src/main/java/com/example/lab/entity/*.java`、`backend/src/main/java/com/example/lab/dto/*.java`、`backend/src/main/java/com/example/lab/controller/*Controller.java`

---

## 代码测试

1. **为 `BorrowService.apply()` 编写单元测试，重点覆盖时间冲突检测的边界场景。包括：完全重叠、部分重叠、首尾相接（startTime == 已有 endTime）、同一设备不同时间段不冲突、不同设备同一时间不冲突等场景。使用 `@DataJpaTest` 或内存 H2 数据库，确保测试可独立运行。**
   - 涉及文件：新增 `backend/src/test/java/com/example/lab/service/BorrowServiceTest.java`

2. **为 `EquipmentService.addEquipment()` 编写单元测试，验证编码生成规则与并发安全性。测试正常场景下生成的编码格式是否为 `LAB{labId}-{seq}`，并验证在并发保存时是否通过锁机制或序列保证编码唯一，不会抛出唯一约束异常。**
   - 涉及文件：新增 `backend/src/test/java/com/example/lab/service/EquipmentServiceTest.java`

---

## 工程化

1. **优化 Backend Dockerfile 的 Maven 依赖缓存。当前 Dockerfile 一次性复制 `pom.xml` + `src` 后执行 `mvn clean package`，导致 `src` 任意改动都会重新下载所有依赖。请改为先单独复制 `pom.xml` 并执行 `mvn dependency:go-offline`，再复制 `src` 编译，提升构建速度。**
   - 涉及文件：`backend/Dockerfile`

2. **为 docker-compose 中的 backend 和 frontend 增加 healthcheck。当前只有 db 服务配置了健康检查，backend 和 frontend 没有。请给 backend 增加基于 `/actuator/health` 或自定义存活端点的 healthcheck，给 frontend 增加 `curl -f http://localhost:3000/` 的 healthcheck，让 Docker/Swarm/K8s 能正确判断服务就绪状态。**
   - 涉及文件：`docker-compose.yml`、`backend/pom.xml`（如引入 actuator）

3. **将 application.yml 中的硬编码数据库密码改为环境变量读取。当前 `spring.datasource.password: 3434` 直接写死在配置文件里，既不安全也不利于多环境部署。请改为 `${MYSQL_PASSWORD:3434}` 默认值模式，并确保 docker-compose 中的 `SPRING_DATASOURCE_PASSWORD` 环境变量能正确传入。**
   - 涉及文件：`backend/src/main/resources/application.yml`、`docker-compose.yml`
