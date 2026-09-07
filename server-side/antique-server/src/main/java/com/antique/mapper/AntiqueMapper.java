package com.antique.mapper;

import com.antique.entity.Antique;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 藏品表 Mapper 接口
 * </p>
 *
 * <p>继承 BaseMapper 自动获得单表 CRUD 方法（selectById/selectPage/insert 等），
 * 计数器更新使用 @Update 原子 SQL，避免并发下先查后改造成的计数丢失。
 *
 * @author shijiu
 */
public interface AntiqueMapper extends BaseMapper<Antique> {

    /**
     * 浏览次数 +1（详情接口调用）
     *
     * <p>使用数据库原子自增而非先查后改：
     * <ul>
     *   <li>避免并发请求下读到的旧值互相覆盖</li>
     *   <li>省一次 SELECT 往返</li>
     * </ul>
     *
     * @param id 藏品 ID
     * @return 受影响行数，0 表示藏品不存在
     */
    @Update("UPDATE antique SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementViewCount(Long id);

    /**
     * 收藏数 +1（收藏接口调用）
     *
     * <p>like_count 为冗余字段，与 favorite 表写入保持同步。
     *
     * @param id 藏品 ID
     * @return 受影响行数，0 表示藏品不存在
     */
    @Update("UPDATE antique SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(Long id);

    /**
     * 收藏数 -1（取消收藏接口调用）
     *
     * <p>{@code AND like_count > 0} 兜底保护：即使因数据异常导致
     * 收藏表记录与计数不一致，也不会把收藏数减成负数。
     *
     * @param id 藏品 ID
     * @return 受影响行数，0 表示藏品不存在或计数已为 0
     */
    @Update("UPDATE antique SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(Long id);
}
