-- 已有库升级：账号密码登录字段（仅需执行一次）
USE campus_team;

ALTER TABLE sys_user
    ADD COLUMN username VARCHAR(64) UNIQUE COMMENT 'login username' AFTER union_id;

ALTER TABLE sys_user
    ADD COLUMN password VARCHAR(128) DEFAULT '' AFTER username;
