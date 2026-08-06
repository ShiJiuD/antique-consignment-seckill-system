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
 * 藏品实体 — 映射数据库 antique 表
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>id: 主键自增</li>
 *   <li>categoryId: 分类（1-瓷器 2-字画 3-玉器 4-铜器 5-杂项，前端写死五个分类）</li>
 *   <li>price: 寄卖价格，DECIMAL(14,2)，最大约 100 万亿</li>
 *   <li>sellerName: 卖家/店铺名称冗余快照，卖家改名不影响历史藏品展示</li>
 *   <li>likeCount: 收藏数冗余字段，收藏/取消收藏时 +1/-1，避免实时 COUNT 收藏表</li>
 *   <li>status: 0-下架，1-在售，2-已售</li>
 *   <li>deletedTime: 逻辑删除时间（NULL=未删除），MyBatis-Plus 全局逻辑删除自动过滤</li>
 * </ul>
 *
 * <h3>自动填充</h3>
 * <p>createTime 和 updateTime 由 {@code MyMetaObjectHandler} 自动填充，
 * 无需手动设置。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("antique")
public class Antique implements Serializable {

    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 藏品名称（如"青花瓷瓶 清代官窑精品"） */
    private String title;

    /** 分类：1-瓷器，2-字画，3-玉器，4-铜器，5-杂项 */
    private Integer categoryId;

    /** 子类别（如"青花瓷"、"粉彩"、"斗彩"） */
    private String subCategory;

    /** 年代（如"清代乾隆年间"、"明代"） */
    private String dynasty;

    /** 材质（如"瓷器"、"青铜"、"和田玉"） */
    private String material;

    /** 寄卖价格（元），DECIMAL(14,2) */
    private BigDecimal price;

    /** 封面图 URL */
    private String coverImage;

    /** 详情图 URL 列表（TEXT 存储 JSON 数组，如 ["https://.../1.jpg"]） */
    private String images;

    /** 藏品描述 */
    private String description;

    /** 卖家用户 ID（关联 user.id） */
    private Long sellerId;

    /** 卖家/店铺名称（冗余快照） */
    private String sellerName;

    /** 浏览次数（详情接口 +1） */
    private Integer viewCount;

    /** 收藏次数（冗余字段，收藏/取消收藏时 +1/-1） */
    private Integer likeCount;

    /** 是否热门：0-否，1-是（首页推荐筛选） */
    private Integer isHot;

    /** 状态：0-下架，1-在售，2-已售 */
    private Integer status;

    /** 创建时间（自动填充） */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充：新增和修改时均更新） */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除时间，NULL=未删除，非NULL=删除时间（MyBatis-Plus 自动过滤） */
    private LocalDateTime deletedTime;
}
