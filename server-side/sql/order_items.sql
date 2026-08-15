-- ============================================
-- 古玩寄卖平台 — 订单明细表
-- ============================================

CREATE TABLE `order_items` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id`      BIGINT UNSIGNED NOT NULL              COMMENT '订单ID（关联 orders.id）',
    `antique_id`    BIGINT UNSIGNED NOT NULL              COMMENT '藏品ID（关联 antique.id）',
    `antique_title` VARCHAR(100)    NOT NULL              COMMENT '藏品名称快照',
    `antique_cover` VARCHAR(255)    DEFAULT NULL          COMMENT '藏品封面图URL快照',
    `dynasty`       VARCHAR(50)     DEFAULT NULL          COMMENT '年代快照',
    `price`         DECIMAL(14,2)   NOT NULL              COMMENT '成交单价快照（元）',
    `quantity`      INT UNSIGNED    NOT NULL DEFAULT 1    COMMENT '购买数量（当前恒为1，多件购买预留）',
    `total_price`   DECIMAL(14,2)   NOT NULL              COMMENT '小计金额（= price × quantity）',
    `created_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    KEY `idx_order_id`   (`order_id`),
    KEY `idx_antique_id` (`antique_id`)

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='订单明细表';
