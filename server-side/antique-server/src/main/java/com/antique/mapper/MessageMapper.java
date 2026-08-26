package com.antique.mapper;

import com.antique.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 消息 Mapper — 操作 message 表
 *
 * <p>继承 BaseMapper 自动获得增删改查（selectById/insert/update...），不用写 SQL。
 * 只有 MyBatis-Plus 自带方法没有的查询，才在这里手写。
 */
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 统计未读消息数（前端角标）
     *
     * <p>注意：自定义 SQL 不走 MyBatis-Plus 的逻辑删除自动拼接，
     * 所以手动补了 deleted_time IS NULL。
     */
    @Select("SELECT COUNT(*) FROM message " +
            "WHERE user_id = #{userId} AND is_read = 0 AND deleted_time IS NULL")
    long countUnread(@Param("userId") Long userId);
}
