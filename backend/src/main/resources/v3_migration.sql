-- =====================================================================
-- SmartTask AI - database migration v3
-- Safe to run more than once. Does NOT delete or change existing data.
--
-- There is NO "USE ..." line on purpose: run it against the database your app
-- actually connects to (the name at the end of spring.datasource.url, default: ptms)
--
--   mysql -u root -p ptms < backend/src/main/resources/v3_migration.sql
--
-- What it adds:
--   1. skills, user_skills, task_skills  (skills a person has / a task needs)
--   2. tasks.estimated_hours             (optional, used by recommendations and risk)
--   3. notifications.dedupe_key          (+ unique index, so a notification is never created twice)
-- =====================================================================

SELECT DATABASE() AS migrating_database;

DROP PROCEDURE IF EXISTS v3_add_column;
DROP PROCEDURE IF EXISTS v3_add_unique_index;
DELIMITER //
CREATE PROCEDURE v3_add_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
CREATE PROCEDURE v3_add_unique_index(IN tbl VARCHAR(64), IN idx VARCHAR(64), IN cols VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND INDEX_NAME = idx) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD UNIQUE KEY `', idx, '` (', cols, ')');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 1. SKILLS ---------------------------------------------------------------
-- (MySQL's default collation is case-insensitive, so "Java" and "java" are the same skill)
CREATE TABLE IF NOT EXISTS skills (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    UNIQUE KEY uk_skill_name (name)
);

CREATE TABLE IF NOT EXISTS user_skills (
    user_id  BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS task_skills (
    task_id  BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, skill_id),
    CONSTRAINT fk_ts_task  FOREIGN KEY (task_id)  REFERENCES tasks(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ts_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
);

-- 2. ESTIMATED HOURS (optional per task) -----------------------------------
CALL v3_add_column('tasks', 'estimated_hours', 'DECIMAL(6,1) NULL');

-- 3. NOTIFICATION DE-DUPLICATION -----------------------------------------
-- NULL for the older notification types; a text key such as OVERDUE:12:2026-03-01 for new ones.
-- MySQL allows many NULLs in a unique index, so existing rows are unaffected.
CALL v3_add_column('notifications', 'dedupe_key', 'VARCHAR(120) NULL');
CALL v3_add_unique_index('notifications', 'uk_notif_dedupe', '`user_id`, `dedupe_key`');

DROP PROCEDURE IF EXISTS v3_add_column;
DROP PROCEDURE IF EXISTS v3_add_unique_index;
