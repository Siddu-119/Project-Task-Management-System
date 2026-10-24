-- =====================================================================
-- ProjectFlow V2.0 - database migration
-- Safe to run more than once. Does NOT delete any existing data.
--
-- Run it against the database your app uses (default: ptms):
--   mysql -u root -p ptms < backend/src/main/resources/v2_migration.sql
--
-- What it does:
--   1. projects : adds status, created_at
--   2. tasks    : status ENUM -> VARCHAR, PENDING -> TODO,
--                 adds priority, created_at, updated_at
--   3. creates project_members, comments, notifications
--   4. fills project_members from tasks that are already assigned
-- =====================================================================

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 1. PROJECTS ---------------------------------------------------------
-- Existing projects become ACTIVE. Progress is NOT stored: it is calculated from tasks.
CALL add_column_if_missing('projects', 'status',     "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
CALL add_column_if_missing('projects', 'created_at', 'DATETIME NULL');
UPDATE projects SET created_at = NOW() WHERE created_at IS NULL;

-- 2. TASKS ------------------------------------------------------------
-- Old column was ENUM('PENDING','IN_PROGRESS','COMPLETED'); it cannot hold TODO/REVIEW.
ALTER TABLE tasks MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'TODO';
UPDATE tasks SET status = 'TODO' WHERE status = 'PENDING';
CALL add_column_if_missing('tasks', 'priority',   "VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'");
CALL add_column_if_missing('tasks', 'created_at', 'DATETIME NULL');
CALL add_column_if_missing('tasks', 'updated_at', 'DATETIME NULL');
UPDATE tasks SET created_at = NOW() WHERE created_at IS NULL;
UPDATE tasks SET updated_at = NOW() WHERE updated_at IS NULL;

-- 3. NEW TABLES -------------------------------------------------------
CREATE TABLE IF NOT EXISTS project_members (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    added_at   DATETIME NULL,
    UNIQUE KEY uk_project_user (project_id, user_id),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    text       TEXT NOT NULL,
    created_at DATETIME NULL,
    CONSTRAINT fk_comment_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       VARCHAR(30) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    task_id    BIGINT NULL,
    project_id BIGINT NULL,
    created_at DATETIME NULL,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. BACKFILL MEMBERS ---------------------------------------------------
-- Anyone already assigned a task in a project becomes a member of that project.
INSERT IGNORE INTO project_members (project_id, user_id, added_at)
SELECT DISTINCT project_id, assigned_to, NOW()
FROM tasks
WHERE project_id IS NOT NULL AND assigned_to IS NOT NULL;

DROP PROCEDURE IF EXISTS add_column_if_missing;
