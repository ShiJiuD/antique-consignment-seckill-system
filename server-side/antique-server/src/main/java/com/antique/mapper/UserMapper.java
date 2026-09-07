package com.antique.mapper;

import com.antique.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

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
}
