-- ============================================
-- 古玩寄卖平台 — 订单主表
-- ============================================

CREATE TABLE `orders` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`         VARCHAR(32)     NOT NULL              COMMENT '订单号（yyyyMMddHHmmss + 6位随机数字）',
    `user_id`          BIGINT UNSIGNED NOT NULL              COMMENT '买家用户ID（关联 user.id）',
    `seller_id`        BIGINT UNSIGNED NOT NULL              COMMENT '卖家用户ID（冗余自 antique.seller_id）',
    `total_amount`     DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '订单总金额（元）',
    `pay_amount`       DECIMAL(14,2)   NOT NULL DEFAULT 0.00 COMMENT '实付金额（V1=总金额，预留优惠/运费）',
    `status`           TINYINT         NOT NULL DEFAULT 0    COMMENT '状态: 0-待付款, 1-待发货, 2-待收货, 3-已完成, 4-已取消',
    `cancel_type`      TINYINT         DEFAULT NULL          COMMENT '取消类型: 1-用户取消, 2-超时取消',
    `pay_deadline`     DATETIME        NOT NULL              COMMENT '支付截止时间（创建时间+16分钟）',
    `receiver_name`    VARCHAR(50)     NOT NULL              COMMENT '收货人姓名',
    `receiver_phone`   VARCHAR(20)     NOT NULL              COMMENT '收货人手机号',
    `receiver_address` VARCHAR(255)    NOT NULL              COMMENT '收货地址',
    `remark`           VARCHAR(255)    DEFAULT NULL          COMMENT '买家备注',
    `pay_time`         DATETIME        DEFAULT NULL          COMMENT '支付时间',
    `ship_time`        DATETIME        DEFAULT NULL          COMMENT '发货时间',
    `receive_time`     DATETIME        DEFAULT NULL          COMMENT '确认收货时间',
    `cancel_time`      DATETIME        DEFAULT NULL          COMMENT '取消时间',
    `finish_time`      DATETIME        DEFAULT NULL          COMMENT '完成时间（与确认收货一致，预留售后期满自动完成）',
    `created_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_time`     DATETIME        DEFAULT NULL          COMMENT '软删除时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no`     (`order_no`),
    KEY `idx_user_id`           (`user_id`),
    KEY `idx_seller_id`         (`seller_id`),
    KEY `idx_user_status`       (`user_id`, `status`),
    KEY `idx_status_created`    (`status`, `pay_deadline`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单主表';
