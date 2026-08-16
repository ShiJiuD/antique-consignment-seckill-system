-- ============================================
-- 古玩寄卖平台 — 消息表
-- ============================================

CREATE TABLE `message` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT UNSIGNED NOT NULL              COMMENT '接收者用户ID（关联 user.id）',
    `type`         TINYINT         NOT NULL DEFAULT 2    COMMENT '消息类型: 1-系统通知, 2-订单消息, 3-AI助手（预留）',
    `title`        VARCHAR(50)     NOT NULL              COMMENT '标题（前端分组展示，如"订单消息"）',
    `content`      VARCHAR(500)    NOT NULL              COMMENT '消息内容',
    `is_read`      TINYINT         NOT NULL DEFAULT 0    COMMENT '是否已读: 0-未读, 1-已读',
    `read_time`    DATETIME        DEFAULT NULL          COMMENT '已读时间',
    `created_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_time` DATETIME        DEFAULT NULL          COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    KEY `idx_user_id`       (`user_id`),
    KEY `idx_user_is_read`  (`user_id`, `is_read`),
    KEY `idx_user_created`  (`user_id`, `created_time`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='消息表';
