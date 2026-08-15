-- ============================================================
-- Carrot 家庭积分系统 表结构（幂等，可重复执行）
-- 设计文档 docs/design.md 3.1
-- ============================================================

-- 用户表（家长账号）
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,                    -- BCrypt
    display_name  TEXT NOT NULL,
    created_at    TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 任务类型表（系统内置 + 自定义）
CREATE TABLE IF NOT EXISTS task_types (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    name             TEXT NOT NULL,
    kind             TEXT NOT NULL DEFAULT 'POSITIVE',  -- POSITIVE=正向任务 / NEGATIVE=惩罚项
    description      TEXT,
    icon             TEXT,                          -- emoji，如 🧹📚
    base_points      INTEGER NOT NULL DEFAULT 0,    -- 正向：完成档积分；惩罚：单次扣分值（存正数）
    good_points      INTEGER NOT NULL DEFAULT 0,    -- 良好档积分（惩罚项不使用）
    excellent_points INTEGER NOT NULL DEFAULT 0,    -- 优秀档积分（惩罚项不使用）
    is_builtin       INTEGER NOT NULL DEFAULT 0,    -- 1=系统内置
    enabled          INTEGER NOT NULL DEFAULT 1,    -- 1=启用
    created_at       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 任务/记录表（登记式：无预创建，记录即完成）
CREATE TABLE IF NOT EXISTS tasks (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    task_type_id     INTEGER REFERENCES task_types(id),  -- 可为 NULL（自定义标题记录）
    title            TEXT NOT NULL,                      -- 冗余标题快照
    description      TEXT,
    base_points      INTEGER NOT NULL DEFAULT 0,         -- 三档积分快照（登记时固化）
    good_points      INTEGER NOT NULL DEFAULT 0,
    excellent_points INTEGER NOT NULL DEFAULT 0,
    status           TEXT NOT NULL DEFAULT 'COMPLETED',  -- COMPLETED=已登记生效 / CANCELLED=已撤销，积分冲正
    tier             INTEGER,                            -- 正向定档：1=完成 2=良好 3=优秀
    earned_points    INTEGER,                            -- 实际入账/扣减积分（正负，生效后非空）
    task_date        TEXT NOT NULL,                      -- 任务执行/完成日期 YYYY-MM-DD（默认当天，可回填）
    completed_at     TEXT,                               -- 家长登记入系统时间
    photo_paths      TEXT,                               -- JSON 数组，相对路径
    remark           TEXT,                               -- 家长备注
    created_at       TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_tasks_status_date ON tasks(status, task_date);

-- 奖励表
CREATE TABLE IF NOT EXISTS rewards (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT,
    points_cost INTEGER NOT NULL,
    type        TEXT NOT NULL DEFAULT 'PHYSICAL',        -- PHYSICAL(实物) / ACTIVITY(活动)
    image_path  TEXT,                                    -- 相对路径
    stock       INTEGER,                                 -- NULL=不限量
    enabled     INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);

-- 兑换记录表
CREATE TABLE IF NOT EXISTS redemptions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    reward_id    INTEGER NOT NULL REFERENCES rewards(id),
    reward_name  TEXT NOT NULL,                          -- 冗余快照
    points_cost  INTEGER NOT NULL,                       -- 冗余快照
    status       TEXT NOT NULL DEFAULT 'PENDING',        -- PENDING / DONE / CANCELLED
    redeemed_at  TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    completed_at TEXT,
    note         TEXT,
    created_at   TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_redemptions_status ON redemptions(status);

-- 积分流水表（唯一账本，只追加）
CREATE TABLE IF NOT EXISTS point_transactions (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    change_amount  INTEGER NOT NULL,                     -- 正=入账 负=扣减
    balance_after  INTEGER NOT NULL,                     -- 变动后余额
    type           TEXT NOT NULL,                        -- TASK / PENALTY / REDEEM / ADJUST
    ref_id         INTEGER,                              -- 关联 tasks.id 或 redemptions.id
    description    TEXT,                                 -- 如「完成任务：刷牙」「兑换：玩具小汽车」
    created_at     TEXT NOT NULL DEFAULT (datetime('now','localtime'))
);
CREATE INDEX IF NOT EXISTS idx_point_txn_created ON point_transactions(created_at);
