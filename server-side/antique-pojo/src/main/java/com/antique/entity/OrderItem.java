package com.antique.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单明细实体 — 映射数据库 order_items 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>antiqueTitle/antiqueCover/dynasty/price: 藏品下单时冗余快照，
 *       藏品后续改名/改价/下架不影响历史订单展示（与 antique.seller_name 冗余思路一致）</li>
 *   <li>quantity: 购买数量，当前业务恒为 1（单件购买），多件购买预留</li>
 *   <li>totalPrice: 小计金额 = price × quantity</li>
 *   <li>明细不可变：无 updated/deleted 字段，订单创建后不做修改</li>
 * </ul>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("order_items")
public class OrderItem implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单 ID（关联 orders.id） */
    private Long orderId;

    /** 藏品 ID（关联 antique.id） */
    private Long antiqueId;

    /** 藏品名称快照 */
    private String antiqueTitle;

    /** 藏品封面图 URL 快照 */
    private String antiqueCover;

    /** 年代快照（如"清代乾隆年间"） */
    private String dynasty;

    /** 成交单价快照（元，下单时的藏品售价） */
    private BigDecimal price;

    /** 购买数量（当前恒为 1，多件购买预留） */
    private Integer quantity;

    /** 小计金额（= price × quantity） */
    private BigDecimal totalPrice;

    /** 创建时间（自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
