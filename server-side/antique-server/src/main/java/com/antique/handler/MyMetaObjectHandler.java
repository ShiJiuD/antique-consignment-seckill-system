package com.antique.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器 — createTime / updateTime
 *
 * <h3>触发时机</h3>
 * <ul>
 *   <li>INSERT 时：自动填充 createTime 和 updateTime 为当前时间</li>
 *   <li>UPDATE 时：自动填充 updateTime 为当前时间</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <p>实体字段上添加 {@code @TableField(fill = FieldFill.INSERT)} 或
 * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解即可，
 * 无需在业务代码中手动设置时间。
 *
 * <h3>参考来源</h3>
 * <p>直接参考 ciTY Art 项目中的同名实现。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     *
     * <p>对带有 fill = FieldFill.INSERT 注解的字段，
     * 如果值为 null，则自动填充为当前时间。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     *
     * <p>对带有 fill = FieldFill.INSERT_UPDATE 注解的字段，
     * 自动填充为当前时间。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
