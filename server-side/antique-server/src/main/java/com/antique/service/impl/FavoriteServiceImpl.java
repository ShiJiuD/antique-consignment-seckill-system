package com.antique.service.impl;

import com.antique.constant.MessageConstant;
import com.antique.entity.Antique;
import com.antique.entity.Favorite;
import com.antique.exception.AuthException;
import com.antique.mapper.AntiqueMapper;
import com.antique.mapper.FavoriteMapper;
import com.antique.service.FavoriteService;
import com.antique.vo.AntiqueCardVO;
import com.antique.vo.PageResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 收藏服务实现 — 收藏/取消收藏/我的收藏列表核心业务逻辑
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl<FavoriteMapper, Favorite>}，
 * 自动获得 save/delete 等 CRUD 方法。
 *
 * <h3>计数同步</h3>
 * <p>收藏表与 antique.like_count 冗余字段的同步约定：
 * <ul>
 *   <li>收藏：favorite INSERT + like_count +1（同一事务）</li>
 *   <li>取消收藏：favorite DELETE + like_count -1（带 like_count > 0 兜底）</li>
 * </ul>
 *
 * <h3>防重复收藏</h3>
 * <ul>
 *   <li>第一道防线：业务预检查（selectCount 判重，返回友好提示）</li>
 *   <li>第二道防线：数据库唯一索引 uk_user_antique（并发兜底）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final AntiqueMapper antiqueMapper;
    private final FavoriteMapper favoriteMapper;

    // ==================== 分页参数默认值 ====================

    /** 默认页码 */
    private static final int DEFAULT_PAGE = 1;

    /** 默认每页数量 */
    private static final int DEFAULT_SIZE = 10;

    /** 每页数量上限 */
    private static final int MAX_SIZE = 50;

    // ========================================================================
    //  接口 4：收藏藏品
    // ========================================================================

    /**
     * 收藏藏品（同一事务：插入收藏记录 + 收藏数 +1）
     * <p>并发下唯一索引 uk_user_antique 兜底防重复。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Long antiqueId) {
        // ----- 步骤 1：校验藏品存在且在售 -----
        // selectById 自动过滤逻辑删除（deleted_time IS NULL）
        Antique antique = antiqueMapper.selectById(antiqueId);
        if (antique == null) {
            throw new AuthException(MessageConstant.ANTIQUE_NOT_EXIST);
        }
        if (antique.getStatus() != 1) {
            throw new AuthException(MessageConstant.ANTIQUE_NOT_ON_SALE);
        }

        // ----- 步骤 2：重复收藏预检查 -----
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getAntiqueId, antiqueId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new AuthException(MessageConstant.FAVORITE_ALREADY);
        }

        try {
            // ----- 步骤 3：插入收藏记录 -----
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setAntiqueId(antiqueId);
            save(favorite);   // createTime 由 MyMetaObjectHandler 自动填充

            // ----- 步骤 4：藏品收藏数 +1（原子自增） -----
            antiqueMapper.incrementLikeCount(antiqueId);
        } catch (DuplicateKeyException e) {
            // 并发场景：两个请求同时通过预检查，唯一索引 uk_user_antique 拦截了后到者
            log.warn("并发收藏被唯一索引拦截: userId={}, antiqueId={}", userId, antiqueId);
            throw new AuthException(MessageConstant.FAVORITE_ALREADY);
        }

        log.info("收藏成功: userId={}, antiqueId={}", userId, antiqueId);
    }

    // ========================================================================
    //  接口 5：取消收藏
    // ========================================================================

    /**
     * 取消收藏（删除记录 → 收藏数 -1，like_count > 0 兜底）
     * <p>未收藏过（删除影响行数为 0）抛业务异常。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long userId, Long antiqueId) {
        // ----- 步骤 1：删除收藏记录 -----
        LambdaQueryWrapper<Favorite> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Favorite::getUserId, userId)
               .eq(Favorite::getAntiqueId, antiqueId);
        int deleted = favoriteMapper.delete(wrapper);

        // 未收藏过该藏品：影响行数为 0
        if (deleted == 0) {
            throw new AuthException(MessageConstant.FAVORITE_NOT_EXIST);
        }

        // ----- 步骤 2：藏品收藏数 -1（原子自减 + 下限保护） -----
        antiqueMapper.decrementLikeCount(antiqueId);

        log.info("取消收藏成功: userId={}, antiqueId={}", userId, antiqueId);
    }

    // ========================================================================
    //  接口 6：获取我的收藏列表
    // ========================================================================

    /**
     * 分页查询我的收藏列表（联表查询，仅卡片字段）
     */
    @Override
    public PageResultVO<AntiqueCardVO> pageFavorites(Long userId, Integer page, Integer size) {
        // ----- 步骤 1：分页参数归一化 -----
        page = normalizePage(page);
        size = normalizeSize(size);

        // ----- 步骤 2：联表分页查询 -----
        // SQL 见 FavoriteMapper.xml，LIMIT 和 COUNT 由分页插件自动生成
        IPage<AntiqueCardVO> result = favoriteMapper.selectFavoriteAntiquePage(new Page<>(page, size), userId);
        log.info("我的收藏查询完成: userId={}, 共 {} 条", userId, result.getTotal());

        // ----- 步骤 3：组装分页结果 -----
        return PageResultVO.<AntiqueCardVO>builder()
                .list(result.getRecords())
                .total(result.getTotal())
                .page(page)
                .size(size)
                .build();
    }

    // ========================================================================
    //  私有工具方法
    // ========================================================================

    /**
     * 页码归一化：null 或小于 1 时取默认值 1
     */
    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 每页数量归一化：null 或小于 1 取默认值 10，超过上限 50 时截断
     */
    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
