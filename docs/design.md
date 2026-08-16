# 家庭积分系统（Carrot）开发设计文档

## 1. 项目概述

### 1.1 项目目标
开发一个运行在 Mac 上的家庭积分管理 Web 应用，主要面向**手机端浏览器**使用（同时兼容电脑端）。用于记录和激励小朋友完成日常任务，通过积分兑换奖励，帮助小朋友养成好习惯。

### 1.2 用户画像与使用场景
- **使用者**：家长（爸爸/妈妈），系统**仅家长登录**，小朋友不登录。
- **场景**：小朋友在现实中完成任务 → 家长在手机上直接登记完成（选择任务类型、根据完成质量定档评分、可拍照）、记录违规扣分 → 积分自动入账 → 小朋友攒够积分兑换奖励，家长发放。
- **规模**：单个家庭、单个小朋友，单机部署，内网（家庭 WiFi）访问。

### 1.3 核心功能
| 模块 | 说明 |
| --- | --- |
| 任务登记 | 正向任务：小朋友完成后家长直接登记（选任务类型 + 定档评分 + 可拍照），积分入账，无需预先创建任务；惩罚项：记录赖床、拖延、玩具不收拾等违规行为直接扣分；任务类型支持内置 + 自定义；记录时支持拍照上传 |
| 积分兑换 | 用积分兑换预设奖励（实物或活动），兑换时校验余额并扣减积分 |
| 数据看板 | 可视化展示当前积分、近 7 天 / 30 天积分变动趋势、最近流水 |
| 用户认证 | 简单的登录保护（家长账号） |

### 1.4 技术栈
| 分类 | 选型 | 说明 |
| --- | --- | --- |
| 后端框架 | Spring Boot 3.x（Java 17） | 主流 Web 框架，快速开发 |
| 前端 | Thymeleaf + Bootstrap 5 + Chart.js | 服务端渲染，无需前后端分离；Chart.js 画趋势图 |
| 数据库 | SQLite（org.xerial:sqlite-jdbc） | 文件型数据库，零运维，适合单机 |
| 数据库访问 | HikariCP + JdbcTemplate | Spring 内置方案，轻量够用 |
| 认证 | Spring Security（表单登录 + BCrypt） | 简单登录保护 |
| 构建 | Maven | 标准 Java 工程 |

### 1.5 非功能需求
- **移动端优先**：页面响应式，触控目标 ≥ 44px，图片上传后压缩缩略。
- **单机内网部署**：`mvn package` 后 `java -jar` 即可运行，无需外部服务。
- **数据可备份**：备份 `data/carrot.db` + `uploads/` 两个目录即完成全量备份。
- **数据可追溯**：积分流水为只追加账本，不做删改，撤销通过冲正记录实现。

---

## 2. 功能设计

### 2.1 整体业务流程

```
家长登录
  ├─ 任务登记：小朋友完成 → 家长选择任务类型 → 定档（完成/良好/优秀）→ 可拍照 → 积分入账
  │            （无需预先创建任务，登记即完成）
  ├─ 违规登记：记录违规（赖床、拖延、玩具不收拾等）→ 选择惩罚项 → 确认扣分 → 积分扣减
  ├─ 积分兑换：浏览奖励 → 兑换（校验余额）→ 生成兑换记录 → 家长发放 → 标记完成
  └─ 数据看板：查看当前积分、近 7/30 天趋势、最近流水
```

### 2.2 任务类型管理

任务类型描述「做什么」，是登记完成/违规时的模板，分为两类：

- **正向任务（POSITIVE）**：好习惯，完成得积分。配置三档积分：`完成档`（base）、`良好档`、`优秀档`，用于家长定档评分。
- **惩罚项（NEGATIVE）**：坏习惯/违规行为（如赖床、拖延），记录一次扣一次分。只需配置**单次扣分值**（存正数），记录时直接扣减，不参与三档定档。

其余规则：
- **系统内置类型**：首次启动自动写入种子数据（见 3.4），不可删除，可停用，可编辑（仅可编辑名称/图标/说明/积分，保留内置标记与启用状态）。
- **自定义类型**：家长可新建（选类型 + 填积分）、编辑、停用。
- 修改任务类型的积分**不影响**已登记的任务（任务登记时快照积分）。

### 2.3 任务登记（核心流程）

**登记式设计**：系统不需要预先创建任务。小朋友在现实中完成任务后，由家长在手机上直接登记完成、定档评分，积分即时入账。

任务记录生命周期：

```
COMPLETED（登记即完成，积分生效）--家长撤销--> CANCELLED（已作废，积分冲正）
```

- **记录完成（正向任务）**：小朋友完成某任务后，家长选择任务类型（或直接写自定义标题），选择执行日期（默认今天，可回填前一天），可上传 1~3 张照片作为完成凭证，然后**选择完成档次**：
  - 档次 1（完成）→ 记 `base_points`
  - 档次 2（良好）→ 记 `good_points`
  - 档次 3（优秀）→ 记 `excellent_points`
  - 确认后立即生成一条 `COMPLETED` 记录，并写入一条 `+积分` 流水（type=`TASK`）。
- **记录违规（惩罚项）**：家长选中某惩罚项记录一次违规，可补充备注，确认后写入一条 `-扣分值` 流水（type=`PENALTY`），积分扣减，同时生成一条记录。
- **撤销记录**：误登记时可撤销（作废）某条已生效记录：状态置为 `CANCELLED`，并追加一条等额反向冲正流水（type=`ADJUST`）——撤销正向任务扣回积分，撤销违规记录退回积分。不做物理删除，保留追溯。
- **查看**：记录列表按「今日 / 本周 / 全部」及状态（已登记 / 已撤销）筛选；正向记录积分以绿色 `+N` 展示，惩罚项以红色 `-N` 展示。

> 说明：登记与定档在同一个操作内完成（家长是唯一操作者，无需两步提交/审核）。若未来开放小朋友登录，可拆分为「小朋友提交完成」→「家长审核定档」两步，数据模型已兼容。

### 2.4 积分与账本

- 积分变动共五种来源：

  | 类型 | 场景 | change_amount |
  | --- | --- | --- |
  | `TASK` | 完成任务入账 | 正数 |
  | `PENALTY` | 记录违规扣分 | 负数 |
  | `REDEEM` | 兑换奖励扣分 | 负数 |
  | `ADJUST` | 手工调整 / 兑换取消退分 / 任务记录撤销冲正 | 可正可负 |
  | `INTEREST` | 账户积分按年化利率复利增长（按天复利） | 正数 |

- 所有变动都写入 `point_transactions` 流水表，**只追加、不修改、不删除**；当前余额 = 最近一条流水的 `balance_after`。
- 需要撤销某笔变动时，追加一条等额反方向记录（冲正），而不是直接删记录。

**小数精度**：积分支持 2 位小数（录入、存储、显示全链路），账本 `change_amount` / `balance_after` 统一保留 2 位小数（HALF_UP）。显示上去尾零：`5` 显示 `5`，`5.5` 显示 `5.5`，`5.25` 显示 `5.25`。

**账户利息（按天复利）**：
- 年化利率配置在 `application.yml` 的 `carrot.interest-rate`（默认 `0.02`，即年化 2%），无页面配置项。
- **惰性结算，无需调度器**：每次加/减积分或查询余额时，用 `interest_state.last_accrual_date` 结算自上次结算日至今的利息，单条 `INTEREST` 流水覆盖整个间隔天数。
- 复利公式：日利率 = 年化/365，`new_balance = round2(balance × (1 + 年化/365)^天数)`，`interest = round2(new_balance - balance)`（对余额舍入，避免逐日舍入的系统偏差）。
- 余额 ≤ 0 时只推进结算日期，不产生流水；同一天重复查询为 no-op（幂等）。

### 2.5 奖励兑换

- **奖励**：实物（如玩具、零食）或活动（如看动画片、去游乐场），配置所需积分、类型、图片（可选）、库存（可不限量）。
- **兑换流程**：家长替小朋友发起兑换 → 校验当前余额 ≥ 奖励所需积分 → 扣减积分（写入 `REDEEM` 流水）→ 生成 `PENDING` 兑换记录。
- 余额不足时提示「积分不足，还差 N 分」。
- **发放**：家长发放奖励后标记为 `DONE`；也可取消 `CANCELLED`（取消时**退回积分**，写入 `ADJUST` 冲正流水）。
- 已下架奖励不可再兑换。

### 2.6 数据看板（首页）

- 顶部展示**当前总积分**。
- 今日概览：今日登记记录数（完成任务 + 违规）、今日积分净变动。
- **积分趋势图**（Chart.js 折线图，7 天 / 30 天可切换）：按天汇总**净积分变动**（入账 - 扣减），可切换为「入账 / 扣减」双系列视图；横轴日期、纵轴积分。
- 最近积分流水（最近 10 条）。
- 最近兑换记录（最近 5 条）。

### 2.7 认证

- Spring Security 表单登录，密码 BCrypt 加密存储。
- 首次启动种子 `admin` / `dad` / `mom` 三个账号（初始密码均 `admin123`，建议登录后修改）；操作日志以登录用户名区分操作人。
- 所有页面除 `/login` 和静态资源外均需登录访问。
- 登录页可选「记住我」：勾选后向 `persistent_logins` 写入持久化令牌，30 天内打开页面自动登录（令牌入库，应用重启仍有效）；退出登录即失效。
- 登录页会自动记住上次输入的用户名（localStorage），密码不落地存储。

---

## 3. 数据模型设计

### 3.1 表结构（SQLite DDL）

```sql
-- 用户表（家长账号，可注册多个家长）
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,                    -- BCrypt
    display_name  TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 记住我令牌表（勾选「记住我」后写入，30 天内免登录）
CREATE TABLE persistent_logins (
    username  VARCHAR(64) NOT NULL,
    series    VARCHAR(64) PRIMARY KEY,
    token     VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
);

-- 任务类型表（系统内置 + 自定义）
CREATE TABLE task_types (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT NOT NULL,
    kind             TEXT NOT NULL DEFAULT 'POSITIVE',  -- POSITIVE=正向任务 / NEGATIVE=惩罚项
    description      TEXT,
    icon             TEXT,                          -- emoji，如 🧹📚
    base_points      REAL NOT NULL DEFAULT 0,    -- 正向：完成档积分；惩罚：单次扣分值（存正数）
    good_points      REAL NOT NULL DEFAULT 0,    -- 良好档积分（惩罚项不使用）
    excellent_points REAL NOT NULL DEFAULT 0,    -- 优秀档积分（惩罚项不使用）
    is_builtin       INTEGER NOT NULL DEFAULT 0,    -- 1=系统内置
    enabled          INTEGER NOT NULL DEFAULT 1,    -- 1=启用
    created_at       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 任务/记录表（登记式：无预创建，记录即完成）
CREATE TABLE tasks (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    task_type_id     INTEGER REFERENCES task_types(id),  -- 可为 NULL（自定义标题记录）
    title            TEXT NOT NULL,                      -- 冗余标题快照
    description      TEXT,
    base_points      REAL NOT NULL DEFAULT 0,         -- 三档积分快照（登记时固化）
    good_points      REAL NOT NULL DEFAULT 0,
    excellent_points REAL NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'COMPLETED',  -- COMPLETED=已登记生效（正向入账或惩罚扣分均生效）/ CANCELLED=已撤销，积分冲正
    tier             INTEGER,                            -- 正向定档：1=完成 2=良好 3=优秀
    earned_points    REAL,                            -- 实际入账/扣减积分（正负，生效后非空）
    task_date        TEXT NOT NULL,                      -- 任务执行/完成日期 YYYY-MM-DD（默认当天，可回填）
    completed_at     TEXT,                               -- 家长登记入系统时间
    photo_paths      TEXT,                               -- JSON 数组，相对路径，如 ["/uploads/tasks/202608/xxx.jpg"]
    remark           TEXT,                               -- 家长备注
    created_by       TEXT,                               -- 操作人用户名（登记人；历史数据为 NULL）
    created_at       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_tasks_status_date ON tasks(status, task_date);

-- 奖励表
CREATE TABLE rewards (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT,
    points_cost REAL NOT NULL,
    type        TEXT NOT NULL DEFAULT 'PHYSICAL',        -- PHYSICAL(实物) / ACTIVITY(活动)
    image_path  TEXT,                                    -- 相对路径
    stock       INTEGER,                                 -- NULL=不限量
    enabled     INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 兑换记录表
CREATE TABLE redemptions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    reward_id    INTEGER NOT NULL REFERENCES rewards(id),
    reward_name  TEXT NOT NULL,                          -- 冗余快照
    points_cost  REAL NOT NULL,                       -- 冗余快照
    status       TEXT NOT NULL DEFAULT 'PENDING',        -- PENDING / DONE / CANCELLED
    redeemed_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    completed_at TEXT,
    note         TEXT,
    created_by   TEXT,                                   -- 操作人用户名（兑换发起人；历史数据为 NULL）
    created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_redemptions_status ON redemptions(status);

-- 积分流水表（唯一账本，只追加）
CREATE TABLE point_transactions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    change_amount  REAL NOT NULL,                     -- 正=入账 负=扣减
    balance_after  REAL NOT NULL,                     -- 变动后余额
    type           TEXT NOT NULL,                        -- TASK / PENALTY / REDEEM / ADJUST / INTEREST
    ref_id         INTEGER,                              -- 关联 tasks.id 或 redemptions.id
    description    TEXT,                                 -- 如「完成任务：刷牙」「兑换：玩具小汽车」
    created_at     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX idx_point_txn_created ON point_transactions(created_at);

-- 利息结算状态（单行表，id=1）：惰性按天复利的上次结算日期
CREATE TABLE interest_state (
    id                INTEGER PRIMARY KEY CHECK (id = 1),
    last_accrual_date TEXT NOT NULL                       -- YYYY-MM-DD，上次利息结算日
);
```

### 3.2 关键设计说明

| 设计点 | 说明 |
| --- | --- |
| 单小朋友 | 按需求仅服务一个小朋友，不做 child 维度；后续扩展只需加 `child_id` 列并加索引 |
| 登记式 | 无预创建任务：任务记录即完成记录，默认状态 `COMPLETED`，登记时即时入账/扣分；撤销走冲正、不做物理删除 |
| 快照冗余 | `tasks` 冗余标题与三档积分、`redemptions` 冗余奖励名与分值，避免历史数据被类型/奖励修改污染 |
| 余额计算 | `point_transactions.balance_after` 冗余存储，当前余额 = `SELECT balance_after FROM point_transactions ORDER BY id DESC LIMIT 1`，O(1) 获取 |
| 账本只追加 | 积分流水不做 UPDATE/DELETE，撤销走冲正（负流水） |
| 时间存储 | 全部使用 SQLite `datetime('now','localtime')`，日期字段存 `YYYY-MM-DD`，与家庭日常习惯一致 |
| 惩罚项 | 惩罚项复用 `task_types` 表，`kind='NEGATIVE'`，仅设单次扣分值（存正数，复用 `base_points`），记录时以负流水（`PENALTY`）入账，不参与三档定档 |

### 3.3 事务约束

- 「登记完成 + 积分入账」必须在一个事务内：先插入任务记录，再插入流水。
- 「撤销记录 + 冲正流水」必须在一个事务内。
- 「兑换 + 扣分 + 生成兑换记录」必须在一个事务内；并发下用事务保证余额不超扣（单用户场景基本无并发，仍有保障）。

### 3.4 内置种子数据（`data.sql`）

**内置任务类型（正向任务 POSITIVE）**：

| 名称           | 完成档 | 良好档 | 优秀档 |
|--------------|-----|-----|-----|
| 按时起床         | 2   | 3   | 5   |
| 自己刷牙洗脸       | 5   | 6   | 8   |
| 整理玩具         | 5   | 6   | 8   |
| 完成作业         | 15  | 20  | 25  |
| 阅读 15 分钟     | 10  | 12  | 15  |
| 帮忙做家务        | 6   | 8   | 10  |
| 早睡           | 5   | 6   | 8   |

**内置惩罚项（NEGATIVE）**：

| 名称      | 单次扣分 |
|---------|-------|
| 赖床      | 5     |
| 洗澡拖延    | 5     |
| 玩具不收拾   | 5     |
| 乱涂乱画    | 10    |

**内置奖励**：

| 名称    | 类型 | 所需积分 |
|-------| --- |------|
| 看动画片 15 分钟 | 活动 | 30   |
| 喝一瓶饮料 | 实物 | 40   |
| 选一个玩具 | 实物 | 100  |
| 周末去游乐场 | 活动 | 200  |


**初始账号**：`admin` / `dad` / `mom`，初始密码均 `admin123`（BCrypt 加密后写入 `users`，各账号独立哈希）。

---

## 4. 页面与路由设计

### 4.1 页面清单

| 页面 | 路径 | 说明 |
| --- | --- | --- |
| 登录页 | `login.html` | 用户名 + 密码表单、「记住我」免登录、记住用户名 |
| 看板（首页） | `dashboard.html` | 当前积分、今日概览、7/30 天趋势图、最近流水、最近兑换 |
| 记录列表 | `tasks/list.html` | 今日 / 本周 / 全部 与状态（已登记/已撤销）筛选，正向与惩罚项分开展示 |
| 记录完成 / 记违规 | `tasks/record.html` | 选择任务类型或惩罚项、执行日期、定档 / 确认扣分、拍照、备注，一键登记 |
| 记录详情 | `tasks/detail.html` | 查看记录、照片与流水，撤销记录（冲正） |
| 任务类型管理 | `task-types.html` / `task-type-form.html` | 内置 + 自定义类型列表；新建/编辑共用表单页（正向任务三档积分 / 惩罚项单次扣分值），含名称、图标、说明；停用 |
| 奖励列表 | `rewards.html` | 奖励卡片（含图片、所需积分），发起兑换；新建/编辑/下架 |
| 兑换记录 | `redemptions.html` | 记录列表，标记发放完成 / 取消 |

> 页面统一使用 Thymeleaf 布局片段（顶栏导航 + 内容区），Bootstrap 5 响应式栅格适配手机端。

### 4.2 路由表

```
GET  /login                       登录页
POST /login                       提交登录（Spring Security 表单）
POST /logout                      退出

GET  /                            数据看板

GET  /tasks                       记录列表（?date=&status= 筛选）
GET  /tasks/record                记录表单（选任务类型/惩罚项，含执行日期、定档）
POST /tasks/record                保存记录（multipart：照片 + tier(正向定档) + task_date + remark）
GET  /tasks/{id}                  记录详情
POST /tasks/{id}/reverse          撤销记录（冲正）

GET  /task-types                  任务类型管理页
GET  /task-types/new              新建任务类型表单
POST /task-types                  新建任务类型
GET  /task-types/{id}/edit        编辑任务类型表单
POST /task-types/{id}/edit        保存任务类型修改
POST /task-types/{id}/toggle      启用/停用

GET  /rewards                     奖励列表
GET  /rewards/new                 新建奖励表单
POST /rewards                     新建奖励
GET  /rewards/{id}/edit           编辑奖励表单
POST /rewards/{id}/edit           保存编辑
POST /rewards/{id}/redeem         发起兑换
POST /rewards/{id}/toggle         上架/下架

GET  /redemptions                 兑换记录
POST /redemptions/{id}/done       标记发放完成
POST /redemptions/{id}/cancel     取消（退回积分）

GET  /uploads/**                  访问上传的图片（静态资源映射）
```

---

## 5. 后端架构设计

### 5.1 项目结构

```
carrot/
├── pom.xml
├── data/                            # 运行时生成：carrot.db
├── uploads/                         # 运行时生成：任务/奖励图片
├── docs/design.md
└── src/main/
    ├── java/com/example/carrot/
    │   ├── CarrotApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java      # 表单登录、URL 权限
    │   │   └── WebConfig.java           # /uploads/** 静态映射
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   ├── DashboardController.java
    │   │   ├── TaskController.java
    │   │   ├── TaskTypeController.java
    │   │   ├── RewardController.java
    │   │   └── RedemptionController.java
    │   ├── service/
    │   │   ├── TaskService.java
    │   │   ├── TaskTypeService.java
    │   │   ├── RewardService.java
    │   │   ├── RedemptionService.java
    │   │   └── PointService.java         # 积分账本，事务核心
    │   ├── repository/
    │   │   ├── TaskRepository.java
    │   │   ├── TaskTypeRepository.java
    │   │   ├── RewardRepository.java
    │   │   ├── RedemptionRepository.java
    │   │   └── PointRepository.java
    │   ├── model/
    │   │   ├── Task.java   ├── TaskType.java   ├── Reward.java
    │   │   ├── Redemption.java   ├── PointTransaction.java   └── User.java
    │   └── dto/
    │       ├── DashboardStats.java        # 看板聚合数据
    │       └── TaskForm.java              # 表单绑定对象
    └── resources/
        ├── application.yml
        ├── schema.sql                     # 建表（spring.sql.init）
        ├── data.sql                       # 种子数据
        ├── static/                        # css / js / img
        └── templates/                     # login/dashboard/tasks/... html
```

### 5.2 分层职责

- **Controller**：接收请求、参数校验、调用 Service、返回 Thymeleaf 视图；文件上传用 `MultipartFile`。
- **Service**：业务规则与事务边界；`PointService` 统一负责积分流水的写入与余额计算。
- **Repository**：JdbcTemplate 封装，SQL 集中在此，返回 `model` 对象。
- **Model**：与表一一对应的 POJO，字段与列名映射（可借助 `BeanPropertyRowMapper`）。

### 5.3 关键技术实现

**SQLite 集成（`application.yml` 关键配置）**

```yaml
spring:
  datasource:
    url: jdbc:sqlite:file:./data/carrot.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always          # 每次启动执行 schema.sql / data.sql（SQL 均幂等）
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 15MB
carrot:
  upload-dir: ./uploads
```

> 依赖：`org.xerial:sqlite-jdbc`（版本随 Spring Boot BOM 管理）。HikariCP 对 SQLite 建议关闭连接校验或配置 `connection-test-query`，避免测试探针报错；单机家庭场景并发极低，无需调优。

**schema.sql / data.sql 幂等性**：建表用 `CREATE TABLE IF NOT EXISTS`；种子数据用 `INSERT OR IGNORE`（按唯一键，如任务类型名），避免重复启动重复插入。

**图片上传与访问**
- 保存：`uploads/tasks/{yyyyMM}/{uuid}.{ext}`（或 `uploads/rewards/...`），存相对路径 `/uploads/tasks/...` 到 `photo_paths`。
- 访问：`WebConfig` 中 `addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadDir + "/")`。
- 前端：`<input type="file" accept="image/*" capture="environment" multiple>` 支持手机直接拍照。
- 展示：完成页缩略图预览；Bootstrap 5 卡片展示。

**认证（SecurityConfig）**
- `formLogin().loginPage("/login").defaultSuccessUrl("/", true)`。
- `authorizeHttpRequests`：`/login`、`/css/**`、`/js/**`、`/img/**`、`/uploads/**` 放行，其余需认证。
- 密码用 `BCryptPasswordEncoder`；种子账号密码也以 BCrypt 写入。
- `rememberMe()`：登录页勾选「记住我」后，`JdbcTokenRepositoryImpl` 向 `persistent_logins` 表写入持久化令牌，`tokenValiditySeconds=30天`，关闭浏览器后打开页面仍自动登录；退出登录时令牌随会话一并清除。

**看板趋势图**
- DashboardController 查询近 30 天每日积分变动，按 `SUM(change_amount)` 得每日净变动，同时按 `type IN ('TASK','PENALTY')` 拆分「入账 / 扣减」两条序列，把 7 天与 30 天两组数据放入 model。
- 模板中用 Thymeleaf 内联 JSON（或 `th:inline="javascript"`）传给 Chart.js 渲染折线图，提供 7/30 天切换按钮。

---

## 6. 开发计划（里程碑）

| 里程碑 | 内容 | 预估 |
| --- | --- | --- |
| **M1 骨架与认证** | pom.xml 依赖、application.yml、schema.sql + data.sql、Spring Security 登录、Thymeleaf 布局与导航、项目可运行 | 0.5~1 天 |
| **M2 任务登记模块** | 任务类型（正向 + 惩罚项）列表/新建/停用；记录完成（选类型 + 定档 + 照片）与记录违规扣分；记录列表 / 详情 / 撤销冲正；积分入账与流水 | 1~2 天 |
| **M3 兑换模块** | 奖励 CRUD 与上/下架；兑换（余额校验 + 扣分）；兑换记录、发放/取消（退回积分） | 0.5~1 天 |
| **M4 看板与打磨** | 首页看板聚合、Chart.js 趋势图、移动端样式打磨、种子数据完善、边界情况（重复提交、并发兑换等） | 1 天 |

> 建议顺序开发：M1 完成后即可本地启动验证；每个里程碑结束都跑一遍关键流程冒烟测试。

---

## 7. 部署与运行

1. 要求：JDK 17 + Maven。
2. 本地运行：IDE 直接运行 `CarrotApplication`，或 `mvn spring-boot:run`。
3. 打包部署：`mvn package -DskipTests` → `java -jar target/carrot-1.0-SNAPSHOT.jar`。
4. 访问：`http://localhost:8080`（手机在同一 WiFi 下访问 `http://<Mac 局域网 IP>:8080`）。
5. 数据目录：首次启动自动生成 `./data/carrot.db` 与 `./uploads/`，**备份时打包这两个目录即可**。
6. 默认账号：`admin` / `dad` / `mom`，初始密码均 `admin123`，登录后建议修改密码。

---

## 8. 风险与注意事项

- **SQLite 并发写**：单写者模型，家庭单机场景无风险；HikariCP 连接池建议配置 `busy_timeout`（如 `PRAGMA busy_timeout=5000`）防止偶发锁冲突。
- **HikariCP 兼容性**：SQLite 不支持 HikariCP 默认的连接测试，需关闭 `connection-test-query` 或用 `org.sqlite.JDBC` 自带校验，避免启动探针报错。
- **重复提交**：登记/兑换接口要做幂等保护（如按钮禁用；撤销接口校验 `status != CANCELLED` 时拒绝重复撤销）。
- **图片体积**：手机拍照原图可能较大，建议前端 `canvas` 压缩或后端限制大小（已配置 5MB），上传后仅存缩略展示。
- **修改积分安全**：积分流水只追加不可改，是后续对账和扩展小朋友登录的基础，不要在其它模块直接 `UPDATE` 流水。
