package com.antique.service;

import com.antique.entity.Favorite;
import com.antique.vo.AntiqueCardVO;
import com.antique.vo.PageResultVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 收藏服务接口 — 收藏/取消收藏/我的收藏列表
 *
 * <p>继承 {@code IService<Favorite>}，自动获得全套 CRUD 方法
 * （save/removeById 等，由实现类继承的 ServiceImpl 提供）。
 * <p>实现类：{@code FavoriteServiceImpl}
 *
 * <h3>计数同步约定</h3>
 * <p>收藏表写入的同时同步维护 antique.like_count 冗余字段：
 * 收藏 +1，取消收藏 -1（带 like_count > 0 兜底保护）。
 */
public interface FavoriteService {

    /**
     * 收藏藏品（校验在售、防重复，成功后收藏数 +1）
     *
     * @param userId    当前用户 ID
     * @param antiqueId 藏品 ID
     */
    void add(Long userId, Long antiqueId);

    /**
     * 取消收藏（未收藏过抛业务异常，成功后收藏数 -1）
     *
     * @param userId    当前用户 ID
     * @param antiqueId 藏品 ID
     */
    void remove(Long userId, Long antiqueId);

    /**
     * 分页查询我的收藏列表（联表查询，仅卡片字段）
     *
     * @param userId 当前用户 ID
     * @param page   页码，缺省或小于 1 默认 1
     * @param size   每页数量，缺省或小于 1 默认 10，最大 50
     * @return 分页卡片列表
     */
    PageResultVO<AntiqueCardVO> pageFavorites(Long userId, Integer page, Integer size);
}
