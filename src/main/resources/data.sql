-- ============================================================
-- Carrot 家庭积分系统 种子数据（幂等，可重复执行）
-- 设计文档 docs/design.md 3.4
-- ============================================================

-- 初始管理员账号：admin / admin123（BCrypt）
INSERT OR IGNORE INTO users (username, password_hash, display_name)
VALUES ('admin', '$2y$10$HLLD/hPcsSNnmuUbot/V2uQkWFErY4DvgvmkYlvEFb.ni5PZdTfPG', '家长');

-- ============================================================
-- 内置正向任务类型（POSITIVE）：完成档 / 良好档 / 优秀档
-- ============================================================
INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '按时起床', 'POSITIVE', '按时自己起床，不赖床', '⏰', 2, 3, 5, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '按时起床' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '自己刷牙洗脸', 'POSITIVE', '独立完成刷牙洗脸', '🪥', 5, 6, 8, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '自己刷牙洗脸' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '整理玩具', 'POSITIVE', '玩完把玩具放回原位', '🧸', 5, 6, 8, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '整理玩具' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '完成作业', 'POSITIVE', '当天作业完成', '📝', 15, 20, 25, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '完成作业' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '阅读 15 分钟', 'POSITIVE', '安静阅读 15 分钟', '📚', 10, 12, 15, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '阅读 15 分钟' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '帮忙做家务', 'POSITIVE', '帮忙做力所能及的家务', '🧹', 6, 8, 10, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '帮忙做家务' AND kind = 'POSITIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin)
SELECT '早睡', 'POSITIVE', '按时上床睡觉', '🌙', 5, 6, 8, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '早睡' AND kind = 'POSITIVE');

-- ============================================================
-- 内置惩罚项（NEGATIVE）：单次扣分值存 base_points（正数）
-- ============================================================
INSERT INTO task_types (name, kind, description, icon, base_points, is_builtin)
SELECT '赖床', 'NEGATIVE', '叫了好几次还不起来', '😴', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '赖床' AND kind = 'NEGATIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, is_builtin)
SELECT '洗澡拖延', 'NEGATIVE', '叫去洗澡迟迟不去', '🚿', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '洗澡拖延' AND kind = 'NEGATIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, is_builtin)
SELECT '玩具不收拾', 'NEGATIVE', '玩完玩具不收拾', '🧸', 5, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '玩具不收拾' AND kind = 'NEGATIVE');

INSERT INTO task_types (name, kind, description, icon, base_points, is_builtin)
SELECT '乱涂乱画', 'NEGATIVE', '在墙上/桌上乱涂乱画', '🖍️', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM task_types WHERE name = '乱涂乱画' AND kind = 'NEGATIVE');

-- ============================================================
-- 内置奖励
-- ============================================================
INSERT INTO rewards (name, description, points_cost, type)
SELECT '看动画片 15 分钟', '安静看完一集动画片', 30, 'ACTIVITY'
WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE name = '看动画片 15 分钟');

INSERT INTO rewards (name, description, points_cost, type)
SELECT '喝一瓶饮料', '任选一瓶饮料', 40, 'PHYSICAL'
WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE name = '喝一瓶饮料');

INSERT INTO rewards (name, description, points_cost, type)
SELECT '选一个玩具', '在玩具店里任选一个', 100, 'PHYSICAL'
WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE name = '选一个玩具');

INSERT INTO rewards (name, description, points_cost, type)
SELECT '周末去游乐场', '周末安排去游乐场玩', 200, 'ACTIVITY'
WHERE NOT EXISTS (SELECT 1 FROM rewards WHERE name = '周末去游乐场');
