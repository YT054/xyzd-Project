-- Activity group chat tables (run once on existing database)
CREATE TABLE IF NOT EXISTS activity_group (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id       BIGINT       NOT NULL,
    name              VARCHAR(128) NOT NULL,
    creator_id        BIGINT       NOT NULL,
    last_message      VARCHAR(512) DEFAULT '',
    last_message_time DATETIME,
    status            TINYINT      NOT NULL DEFAULT 1,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity (activity_id),
    INDEX idx_creator (creator_id)
);

CREATE TABLE IF NOT EXISTS activity_group_member (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id       BIGINT   NOT NULL,
    user_id        BIGINT   NOT NULL,
    role           TINYINT  NOT NULL DEFAULT 0,
    last_read_time DATETIME,
    joined_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user (user_id)
);

CREATE TABLE IF NOT EXISTS activity_group_message (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id     BIGINT NOT NULL,
    sender_id    BIGINT NOT NULL,
    content      TEXT   NOT NULL,
    message_type TINYINT NOT NULL DEFAULT 1,
    status       TINYINT NOT NULL DEFAULT 1,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group (group_id, created_at)
);
