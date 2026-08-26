package com.antique.mapper;

import com.antique.entity.UserSignRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 签到流水表 Mapper 接口
 * </p>
 *
 * <p>继承 BaseMapper 自动获得单表 CRUD 方法（insert/selectOne/selectCount 等）。
 * 唯一索引 {@code uk_user_date(user_id, sign_date)} 由数据库保证，
 * 本表无需自定义 SQL（连续天数回推在 Service 层用 LambdaQueryWrapper 完成）。
 *
 * @author shijiu
 */
public interface UserSignRecordMapper extends BaseMapper<UserSignRecord> {
}
