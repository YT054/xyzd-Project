-- 校园组队通 Campus Team System
-- MySQL 8.0.33
CREATE DATABASE IF NOT EXISTS campus_team DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_team;

-- ==================== RBAC ====================
CREATE TABLE sys_role (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code   VARCHAR(32)  NOT NULL UNIQUE COMMENT 'USER/CREATOR/CAMPUS_ADMIN/OPS_ADMIN',
    role_name   VARCHAR(64)  NOT NULL,
    description VARCHAR(256),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '角色表';

CREATE TABLE sys_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    perm_code   VARCHAR(64)  NOT NULL UNIQUE,
    perm_name   VARCHAR(64)  NOT NULL,
    module      VARCHAR(32)  NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '权限表';

CREATE TABLE sys_role_permission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) COMMENT '角色权限关联';

-- ==================== 用户 ====================
CREATE TABLE sys_user (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    openid            VARCHAR(64)  UNIQUE,
    union_id          VARCHAR(64),
    username          VARCHAR(64)  UNIQUE,
    password          VARCHAR(128) DEFAULT '' COMMENT 'BCrypt，账号注册用户',
    nickname          VARCHAR(64)  NOT NULL DEFAULT '',
    avatar            VARCHAR(512) DEFAULT '',
    phone             VARCHAR(20)  DEFAULT '',
    student_no        VARCHAR(32)  DEFAULT '',
    college           VARCHAR(64)  DEFAULT '',
    gender            TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    privacy_level     TINYINT      NOT NULL DEFAULT 0 COMMENT '0公开 1仅同活动可见 2私密',
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    profile_completed TINYINT      NOT NULL DEFAULT 0 COMMENT '0未完善 1已完善',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_openid (openid),
    INDEX idx_status (status)
) COMMENT '小程序用户';

CREATE TABLE sys_user_role (
    id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
) COMMENT '用户角色';

CREATE TABLE sys_admin (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL UNIQUE,
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt',
    real_name   VARCHAR(64)  DEFAULT '',
    role_id     BIGINT       NOT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    last_login_ip VARCHAR(64) DEFAULT '',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '管理后台账号';

-- ==================== 活动 ====================
CREATE TABLE activity_category (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(64) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    status     TINYINT     NOT NULL DEFAULT 1,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '活动分类';

CREATE TABLE activity (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    creator_id        BIGINT       NOT NULL,
    category_id       BIGINT       NOT NULL,
    title             VARCHAR(128) NOT NULL,
    description       TEXT,
    cover_image       VARCHAR(512) DEFAULT '',
    location          VARCHAR(256) DEFAULT '',
    start_time        DATETIME     NOT NULL,
    end_time          DATETIME     NOT NULL,
    register_deadline DATETIME     NOT NULL,
    max_members       INT          NOT NULL DEFAULT 0 COMMENT '0不限',
    current_members   INT          NOT NULL DEFAULT 0,
    recruit_status    TINYINT      NOT NULL DEFAULT 1 COMMENT '1招募中 2已满员 3已停止',
    activity_status   TINYINT      NOT NULL DEFAULT 1 COMMENT '1招募中 2进行中 3已结束 4已下架',
    tags              VARCHAR(256) DEFAULT '',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_creator (creator_id),
    INDEX idx_category (category_id),
    INDEX idx_status (activity_status),
    INDEX idx_start_time (start_time),
    FULLTEXT INDEX ft_title_desc (title, description)
) COMMENT '活动';

-- ==================== 报名 ====================
CREATE TABLE activity_registration (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id   BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '0待审核 1通过 2拒绝 3已取消',
    apply_message VARCHAR(512) DEFAULT '',
    audit_message VARCHAR(512) DEFAULT '',
    audited_at    DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity_user (activity_id, user_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) COMMENT '活动报名';

-- ==================== 私信 ====================
CREATE TABLE chat_conversation (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id       BIGINT       NOT NULL,
    user1_id          BIGINT       NOT NULL,
    user2_id          BIGINT       NOT NULL,
    last_message      VARCHAR(512) DEFAULT '',
    last_message_time DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv (activity_id, user1_id, user2_id),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id)
) COMMENT '私信会话';

CREATE TABLE chat_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    activity_id     BIGINT NOT NULL,
    sender_id       BIGINT NOT NULL,
    receiver_id     BIGINT NOT NULL,
    content         TEXT   NOT NULL,
    is_read         TINYINT NOT NULL DEFAULT 0,
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv (conversation_id),
    INDEX idx_receiver_unread (receiver_id, is_read)
) COMMENT '私信消息';

-- ==================== 活动群聊 ====================
CREATE TABLE activity_group (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id       BIGINT       NOT NULL,
    name              VARCHAR(128) NOT NULL,
    creator_id        BIGINT       NOT NULL,
    last_message      VARCHAR(512) DEFAULT '',
    last_message_time DATETIME,
    status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0解散',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_activity (activity_id),
    INDEX idx_creator (creator_id)
) COMMENT '活动群聊';

CREATE TABLE activity_group_member (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id       BIGINT   NOT NULL,
    user_id        BIGINT   NOT NULL,
    role           TINYINT  NOT NULL DEFAULT 0 COMMENT '0成员 1群主',
    last_read_time DATETIME,
    joined_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_user (user_id)
) COMMENT '群成员';

CREATE TABLE activity_group_message (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id     BIGINT NOT NULL,
    sender_id    BIGINT NOT NULL,
    content      TEXT   NOT NULL,
    message_type TINYINT NOT NULL DEFAULT 1 COMMENT '1文本 2系统',
    status       TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_group (group_id, created_at)
) COMMENT '群消息';

-- ==================== 打卡与评价 ====================
CREATE TABLE activity_checkin (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id  BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    content      VARCHAR(1024) DEFAULT '',
    checkin_time DATETIME     NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_checkin (activity_id, user_id),
    INDEX idx_activity (activity_id)
) COMMENT '活动打卡';

CREATE TABLE activity_review (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    rating      TINYINT      NOT NULL COMMENT '1-5星',
    content     VARCHAR(1024) DEFAULT '',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0已删除',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_review (activity_id, user_id),
    INDEX idx_activity (activity_id)
) COMMENT '活动评价';

-- ==================== 投诉与日志 ====================
CREATE TABLE user_complaint (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    reporter_id  BIGINT       NOT NULL,
    target_type  TINYINT      NOT NULL COMMENT '1活动 2评价 3私信 4用户',
    target_id    BIGINT       NOT NULL,
    reason       VARCHAR(512) NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2驳回',
    handler_id   BIGINT,
    handle_remark VARCHAR(512) DEFAULT '',
    handled_at   DATETIME,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) COMMENT '用户投诉';

CREATE TABLE sys_operation_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT,
    operator_type TINYINT NOT NULL COMMENT '1用户 2管理员',
    module      VARCHAR(32) NOT NULL,
    action      VARCHAR(64) NOT NULL,
    detail      TEXT,
    ip          VARCHAR(64) DEFAULT '',
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志';

-- ==================== 初始数据 ====================
INSERT INTO sys_role (role_code, role_name, description) VALUES
('USER', '普通师生用户', '浏览、报名、私信、打卡、评价'),
('CREATOR', '活动发起者', '继承普通用户，可管理自己发布的活动'),
('CAMPUS_ADMIN', '校园管理员', '内容监管、投诉处理、数据统计'),
('OPS_ADMIN', '系统运维管理员', '系统运维，无业务编辑权限');

INSERT INTO sys_permission (perm_code, perm_name, module) VALUES
('activity:view', '浏览活动', 'activity'),
('activity:publish', '发布活动', 'activity'),
('activity:manage', '管理活动', 'activity'),
('registration:apply', '报名活动', 'registration'),
('registration:audit', '审核报名', 'registration'),
('chat:send', '发送私信', 'chat'),
('checkin:submit', '活动打卡', 'checkin'),
('review:submit', '提交评价', 'review'),
('admin:content', '内容监管', 'admin'),
('admin:user', '用户管理', 'admin'),
('admin:stats', '数据统计', 'admin'),
('ops:system', '系统运维', 'ops');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'USER' AND p.perm_code IN ('activity:view','registration:apply','chat:send','checkin:submit','review:submit');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'CREATOR' AND p.perm_code IN ('activity:view','activity:publish','activity:manage','registration:apply','registration:audit','chat:send','checkin:submit','review:submit');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'CAMPUS_ADMIN' AND p.perm_code IN ('activity:view','admin:content','admin:user','admin:stats');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.role_code = 'OPS_ADMIN' AND p.perm_code = 'ops:system';

INSERT INTO activity_category (name, sort_order) VALUES
('学术竞赛', 1), ('体育运动', 2), ('志愿服务', 3), ('文艺活动', 4), ('社团招新', 5), ('其他', 99);

-- 默认管理员 admin/admin123 (BCrypt)
INSERT INTO sys_admin (username, password, real_name, role_id)
SELECT 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员',
       id FROM sys_role WHERE role_code = 'CAMPUS_ADMIN';

INSERT INTO sys_admin (username, password, real_name, role_id)
SELECT 'ops', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '运维管理员',
       id FROM sys_role WHERE role_code = 'OPS_ADMIN';
