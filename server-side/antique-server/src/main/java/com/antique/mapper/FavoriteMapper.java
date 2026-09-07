package com.antique.mapper;

import com.antique.entity.Favorite;
import com.antique.vo.AntiqueCardVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 收藏表 Mapper 接口
 * </p>
 *
 * <p>继承 BaseMapper 自动获得单表 CRUD 方法。
 * "我的收藏"需要联表查询藏品卡片字段，自定义 SQL 见
 * {@code src/main/resources/mapper/FavoriteMapper.xml}。
 *
 * @author shijiu
 */
public interface FavoriteMapper extends BaseMapper<Favorite> {

    /**
     * 分页查询我的收藏列表（favorite 联表 antique）
     *
     * <p>SQL 实现见 FavoriteMapper.xml，分页由 MyBatis-Plus 分页插件
     * 自动生成 LIMIT 和 COUNT 语句（需 MybatisPlusInterceptor 支持）。
     *
     * @param page   分页参数（页码、每页数量）
     * @param userId 当前用户 ID
     * @return 当前页的藏品卡片数据
     */
    IPage<AntiqueCardVO> selectFavoriteAntiquePage(IPage<AntiqueCardVO> page, @Param("userId") Long userId);
}
