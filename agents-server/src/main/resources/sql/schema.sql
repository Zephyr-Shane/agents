-- =============================================
-- AI Agent Mini Program - Database Schema
-- =============================================

CREATE DATABASE IF NOT EXISTS agents DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE agents;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid`        VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '微信openid',
    `nickname`      VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`        VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
    `phone`         VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '手机号',
    `register_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `last_login`    DATETIME     NULL COMMENT '最后登录时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-未删 1-已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 智能体表
CREATE TABLE IF NOT EXISTS `agent` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '智能体ID',
    `creator_id`      BIGINT       NOT NULL COMMENT '创建者用户ID',
    `name`            VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '智能体名称',
    `description`     VARCHAR(256) NOT NULL DEFAULT '' COMMENT '一句话描述',
    `avatar`          VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像URL',
    `current_version` INT          NOT NULL DEFAULT 1 COMMENT '当前版本号',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-已发布',
    `is_public`       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否公开 0-私密 1-公开',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体表';

-- 智能体版本快照表
CREATE TABLE IF NOT EXISTS `agent_version` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '版本ID',
    `agent_id`       BIGINT       NOT NULL COMMENT '智能体ID',
    `version`        INT          NOT NULL COMMENT '版本号(自增)',
    `system_prompt`  TEXT         NOT NULL COMMENT '完整系统提示词',
    `features_json`  JSON         NULL COMMENT '特性配置快照(知识库/联网/回答约束等)',
    `description`    VARCHAR(256) NOT NULL DEFAULT '' COMMENT '该版本描述',
    `change_log`     VARCHAR(512) NOT NULL DEFAULT '' COMMENT '修改说明',
    `created_by`     BIGINT       NOT NULL COMMENT '修改人用户ID',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent_version` (`agent_id`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能体版本快照表';

-- 用户-智能体关联表
CREATE TABLE IF NOT EXISTS `user_agent` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `user_id`         BIGINT   NOT NULL COMMENT '用户ID',
    `agent_id`        BIGINT   NOT NULL COMMENT '智能体ID',
    `is_creator`      TINYINT  NOT NULL DEFAULT 0 COMMENT '是否是创建者 0-否 1-是',
    `is_favorite`     TINYINT  NOT NULL DEFAULT 0 COMMENT '是否收藏 0-否 1-是',
    `first_used_time` DATETIME NULL COMMENT '首次使用时间',
    `last_used_time`  DATETIME NULL COMMENT '最后使用时间',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_agent` (`user_id`, `agent_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-智能体关联表';

-- 会话表
CREATE TABLE IF NOT EXISTS `conversation` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `agent_id`    BIGINT       NULL COMMENT '智能体ID(NULL=通用对话)',
    `title`       VARCHAR(128) NOT NULL DEFAULT '新的对话' COMMENT '会话标题',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_agent` (`user_id`, `agent_id`),
    KEY `idx_update` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `conversation_id` BIGINT   NOT NULL COMMENT '会话ID',
    `role`            VARCHAR(16) NOT NULL DEFAULT 'user' COMMENT '角色 user/assistant',
    `content`         TEXT     NOT NULL COMMENT '消息内容',
    `tokens`          INT      NOT NULL DEFAULT 0 COMMENT '消耗Token数',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_conversation` (`conversation_id`),
    KEY `idx_create` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 知识库文档表
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `agent_id`    BIGINT       NOT NULL COMMENT '关联智能体ID',
    `file_name`   VARCHAR(256) NOT NULL DEFAULT '' COMMENT '文件名',
    `file_url`    VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '文件URL',
    `file_type`   VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '文件类型 pdf/word/txt',
    `file_size`   BIGINT       NOT NULL DEFAULT 0 COMMENT '文件大小(字节)',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_agent` (`agent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';
