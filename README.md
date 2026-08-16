# 🥕 Carrot 家庭积分系统

一个运行在 Mac 上的家庭积分管理 Web 应用，主要面向**手机端浏览器**使用（同时兼容电脑端）。
用于记录和激励小朋友完成日常任务，通过积分兑换奖励，帮助小朋友养成好习惯。

- 仅家长登录，小朋友不登录
- 单个家庭 / 单个小朋友，单机内网（家庭 WiFi）部署
- 无需外部服务，`java -jar` 即可运行，数据零运维

## 功能特性

| 模块 | 说明 |
| --- | --- |
| 任务登记 | 正向任务：小朋友完成后家长直接登记（选任务类型 + 定档评分 + 可拍照，1~3 张），积分即时入账，无需预先创建任务；惩罚项：记录赖床、拖延等违规行为直接扣分；任务类型支持内置 + 自定义 |
| 积分兑换 | 用积分兑换预设奖励（实物或活动），兑换时校验余额并扣减积分；可标记发放、可取消（退回积分） |
| 数据看板 | 当前积分、今日概览、7 / 30 天积分趋势图（Chart.js，支持净变动 / 入账-扣减双系列）、最近流水与兑换 |
| 用户认证 | Spring Security 表单登录 + BCrypt 密码加密 |
| 小数积分 | 积分支持 2 位小数（如 5.5、0.25），全链路录入 / 存储 / 显示 |
| 积分利息 | 账户积分按年化利率按天复利增长（默认年化 2%，配置文件可改，惰性结算、无调度器） |
| 数据可追溯 | 积分流水为只追加账本，撤销/取消通过冲正记录实现，不做物理删除 |

## 技术栈

- **后端**：Spring Boot 3.4.x（Java 17）、Spring Security、Spring JDBC（JdbcTemplate）
- **前端**：Thymeleaf + thymeleaf-layout-dialect、Bootstrap 5、Chart.js
- **数据库**：SQLite（文件型，零运维）
- **构建**：Maven

> Bootstrap 5 与 Chart.js 均已本地化到 `src/main/resources/static/vendor/`，**离线可用**，部署不需要外网。

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 运行

```bash
# 方式一：开发模式（热加载，模板改动即时生效）
mvn spring-boot:run

# 方式二：打包部署（推荐）
mvn package -DskipTests
java -jar target/carrot-1.0-SNAPSHOT.jar
```

访问：`http://localhost:8080`
手机与 Mac 在同一 WiFi 下，访问 `http://<Mac 局域网 IP>:8080`

### 默认账号

首次启动自动创建三个账号（初始密码均为 `admin123`，建议登录后自行修改密码）：

| 账号 | 说明 |
| --- | --- |
| `admin` | 家长（通用） |
| `dad` | 爸爸 |
| `mom` | 妈妈 |

各账号以用户名登录，操作日志会记录操作人用户名，便于区分「是谁做了什么」。

## 主要页面

| 页面 | 路径 |
| --- | --- |
| 登录 | `/login` |
| 数据看板（首页） | `/` |
| 记录列表（今日 / 本周 / 全部 × 已登记 / 已撤销） | `/tasks` |
| 登记记录（完成 / 违规） | `/tasks/record` |
| 任务类型管理（新建 / 编辑 / 停用） | `/task-types` |
| 奖励列表 / 兑换 | `/rewards` |
| 兑换记录 | `/redemptions` |

## 数据与备份

所有数据都在这两个目录，**备份时打包它们即完成全量备份**：

```
data/carrot.db      # SQLite 数据库（账号、类型、记录、奖励、兑换、积分流水）
uploads/            # 上传的任务/奖励图片
```

将应用升级到新机器时，只需拷贝这两个目录到新目录，再重新 `java -jar` 即可。

> 积分流水是只追加账本：撤销任务、取消兑换都不会删除流水，而是追加等额反向的冲正流水（`ADJUST`），保证余额恒等于最近一条流水的 `balance_after`，全程可对账。

## 项目结构

```
carrot/
├── docs/design.md              # 开发设计文档（需求 / 数据模型 / 路由）
├── data/                       # 运行时生成：carrot.db（已 gitignore）
├── uploads/                    # 运行时生成：任务/奖励图片（已 gitignore）
├── pom.xml
└── src/main/
    ├── java/com/example/carrot/
    │   ├── CarrotApplication.java
    │   ├── config/             # SecurityConfig / WebConfig
    │   ├── controller/         # 看板 / 任务 / 任务类型 / 奖励 / 兑换 / 认证
    │   ├── service/            # 业务逻辑与事务边界（积分账本、兑换、看板聚合…）
    │   ├── repository/         # JdbcTemplate 数据访问
    │   ├── model/              # 与表对应的 POJO
    │   └── dto/                # 表单与看板聚合对象
    └── resources/
        ├── application.yml
        ├── schema.sql / data.sql   # 建表 + 种子数据（幂等）
        ├── static/                 # css / js / vendor（Bootstrap、Chart.js）
        └── templates/              # Thymeleaf 页面
```

## 常见问题

- **手机拍照后上传被拒**：单张不超过 5MB，单次最多 3 张。
- **登录后回到登录页**：检查是否用了 `admin` / `dad` / `mom` 且密码为 `admin123`；密码重置需直接改 `data/carrot.db` 中 `users.password_hash` 的 BCrypt 值。
- **想从头初始化**：停止应用，删除 `data/` 与 `uploads/`，重新 `java -jar` 即可（会自动重建种子数据）。
