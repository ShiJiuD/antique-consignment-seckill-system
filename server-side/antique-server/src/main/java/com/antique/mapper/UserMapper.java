package com.antique.mapper;

import com.antique.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户 Mapper — 继承 MyBatis-Plus BaseMapper
 *
 * <p>BaseMapper 已提供常用 CRUD 方法：
 * <ul>
 *   <li>selectById / selectList / selectOne</li>
 *   <li>insert</li>
 *   <li>updateById</li>
 *   <li>deleteById（配合逻辑删除配置）</li>
 * </ul>
 *
 * <p>如需自定义 SQL（如复杂连表查询），可在 resources/mapper/UserMapper.xml 中定义。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 签到成功后的用户信息更新（原子 SQL，与流水表插入同一事务）
     *
     * <p>连续天数由 Service 层按 MySQL 签到流水回源算好后直接赋值
     * （昨天签过 → 昨天的连续 + 1；断签 → 1），此处不做 IF 判断。
     *
     * @param userId     用户 ID
     * @param points     本次签到获得的积分（2 × 连续天数）
     * @param signInDays 签到后的连续签到天数
     * @return 受影响行数，0 表示用户不存在
     */
    @Update("""
            UPDATE user SET
                points = points + #{points},
                sign_in_days = #{signInDays},
                last_sign_date = CURDATE()
            WHERE id = #{userId}
            """)
    int updateSignInfo(@Param("userId") Long userId,
                       @Param("points") Integer points,
                       @Param("signInDays") Integer signInDays);
}
