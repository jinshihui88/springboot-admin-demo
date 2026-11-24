# springboot-admin-demo

Spring Boot 3 整合 Spring Boot Admin（SBA）实战教程项目：多模块聚合工程，包含一个 Admin Server（可视化监控中心）和一个 Admin Client（被监控的业务应用），完整演示注册、健康监控、JVM/线程观测、日志动态调级与依赖异常联动。

## 1、项目简介

- **admin-server**：Spring Boot Admin 服务端，提供 Web 管理界面，接收客户端注册并聚合监控数据，端口 `8080`。
- **admin-client**：被监控的业务应用（订单 CRUD + 监控演示接口），引入 SBA Client 后自动注册到 Server，端口 `8081`。

## 2、技术栈与版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17.0.12 | |
| Maven | 3.9.3 | 多模块聚合构建 |
| Spring Boot | 3.5.16 | 父 POM `spring-boot-starter-parent` |
| Spring Boot Admin | 3.5.11 | 3.5.x 线适配 Spring Boot 3.5.x（该线编译基线为 Boot 3.5.14）；4.x 基于 Spring Boot 4，Boot 3 项目勿用 |
| Micrometer | 由 Boot BOM 管理 | Actuator 底层指标门面 |

版本核实来源：
- Maven Central：`de.codecentric/spring-boot-admin-starter-server`（`maven-metadata.xml` 中 3.5.x 线最新为 3.5.11，`spring-boot-admin-server-3.5.11.pom` 依赖 `spring-boot-starter 3.5.14`）
- 官方文档：https://docs.spring-boot-admin.com/3.5.0/docs/installation-and-setup

## 3、快速开始

### 3.1 环境要求

- JDK 17+、Maven 3.9+
- 端口 8080（Server）、8081（Client）未被占用

### 3.2 构建

```bash
cd D:/project/springboot-admin-demo
mvn clean package
```

### 3.3 启动（先 Server 后 Client）

```bash
java -jar admin-server/target/admin-server-1.0.0.jar
java -jar admin-client/target/admin-client-1.0.0.jar
```

### 3.4 验证

- 浏览器打开 `http://localhost:8080/`，Wallboard 中出现 `admin-client` 且状态 UP
- 端到端验证脚本（需双应用已启动）：

```bash
python tools/verify_endpoints.py
```

## 4、工程结构

```
springboot-admin-demo
├── pom.xml                          # 聚合 POM：统一 Boot 3.5.16 + SBA 3.5.11
├── admin-server                     # 服务端（8080）
│   ├── pom.xml
│   └── src/main/java/com/example/adminserver/AdminServerApplication.java
│   └── src/main/resources/application.yml
├── admin-client                     # 客户端（8081）
│   ├── pom.xml
│   └── src/main/java/com/example/adminclient
│       ├── AdminClientApplication.java
│       ├── common/                  # ApiResult、GlobalExceptionHandler
│       ├── controller/              # OrderController、MonitorDemoController
│       ├── dto/                     # OrderCreateRequest、OrderResponse
│       ├── exception/               # ResourceNotFoundException
│       ├── health/                  # PaymentChannelHealthIndicator（可切换 UP/DOWN）
│       └── repository/              # OrderRepository（内存）
│   └── src/main/resources/application.yml
└── tools/                           # verify_endpoints.py 端到端验证脚本
```

## 5、关键配置说明

### 5.1 admin-server

- `@EnableAdminServer`：开启注册中心、聚合与 UI。
- `spring.thymeleaf.check-template-location: false`：Admin UI 由 `spring-boot-admin-server-ui` 自带，不走 `classpath:/templates/`，关闭检查可消除启动 WARN。

### 5.2 admin-client

```yaml
spring:
  boot:
    admin:
      client:
        url: http://localhost:8080        # Server 地址，按周期注册
        instance:
          name: admin-client              # 界面展示名
          metadata:
            team: platform
            env: dev

management:
  endpoints:
    web:
      exposure:
        include: "*"                      # 放开全部端点（生产按需收敛）
  endpoint:
    health:
      show-details: always                # 组件级健康详情
  info:
    env:
      enabled: true                       # Boot 2.6+ info 端点默认不暴露 env
```

## 6、接口与功能清单

### 6.1 业务接口（admin-client，8081）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/orders` | 创建订单（参数校验，失败 400） |
| GET | `/api/orders` | 订单列表 |
| GET | `/api/orders/{id}` | 订单详情（不存在 404） |
| POST | `/api/demo/dependency?healthy=false` | 切换模拟依赖健康状态（DOWN/UP） |
| GET | `/api/demo/log?level=debug` | 按级别输出演示日志（验证动态调级） |

### 6.2 监控能力（SBA 界面 + Actuator 端点）

- 应用列表 / 状态总览（Wallboard）
- 实例详情：JVM 内存、线程、CPU、垃圾回收
- Environment / Beans / Mappings / Conditions / ConfigProps
- 日志级别动态调整（`/actuator/loggers` + UI 操作，不重启生效）
- 健康状态联动：依赖异常 → 应用 DOWN → Server 界面同步 DOWN → 恢复 UP

## 7、踩坑备忘

1. **Admin Server 启动 Thymeleaf WARN**：SBA UI 不从 `classpath:/templates/` 加载，Boot 自动配置默认检查会打 WARN，`spring.thymeleaf.check-template-location=false` 消除。
2. **`/instances` API 的 Accept 协商**：请求头带 `application/hal+json` 会 406；用 `application/json` 即可返回实例数组。UI 首页 `/` 必须带 `text/html`（或 `*/*`），纯 JSON Accept 同样 406。
3. **健康为 DOWN 时 HTTP 状态是 503**：`/actuator/health` 的 HTTP 状态码跟随整体健康状态（UP=200，DOWN=503），body 仍是完整 JSON；判断健康要看 body 的 `status` 字段而不是只看 HTTP 码。
4. **Spring Boot Test 默认行为**：`TestRestTemplate` 默认 Accept 可能协商到 406，断言 `/instances` 时需显式 `Accept: application/json`。
5. **Server 停机时 Client 注册失败只打 WARN**：`DefaultApplicationRegistrator` 按周期重试，Server 恢复后自动注册成功，无需重启 Client。
6. **全局异常处理器限定包**：`@RestControllerAdvice(basePackages = "...controller")`，避免把 Actuator/SBA 端点的框架异常包装成业务 500。

## 8、生产化建议

- **安全**：Server 界面与 Client 端点都要加 Spring Security 保护；Client 端点 `include: "*"` 必须收敛（至少去掉 env/beans/shutdown 类）。
- **接入方式**：微服务环境建议改用注册中心发现（Eureka/Consul/Nacos），Client 无需逐个配置 Server 地址。
- **告警通知**：SBA Server 支持状态变化事件，可接邮件（`spring-boot-admin-starter-mail`）、钉钉/企微机器人等，对 DOWN/OFFLINE 事件告警。
- **持久化**：默认注册信息在内存，Server 重启后等 Client 重新注册；需要历史数据请对接 Prometheus + Grafana（SBA 与它们互补而非替代）。
- **HTTPS**：生产环境 Client 的 management 端点与 Server UI 都应走 HTTPS，注意 `instance.service-base-url`/`management-base-url` 与实际访问协议一致。

## 9、验证情况

- `mvn clean package`：BUILD SUCCESS，7 个集成测试（server 3 + client 4）Failures 0 / Errors 0
- `tools/verify_endpoints.py`：TOTAL=30 PASS=30 FAIL=0
- admin-server 启动日志零 ERROR/WARN；admin-client 除演示用 `log.warn`（健康切换提示）与一次 Server 重启窗口内的注册重试 WARN 外无异常
