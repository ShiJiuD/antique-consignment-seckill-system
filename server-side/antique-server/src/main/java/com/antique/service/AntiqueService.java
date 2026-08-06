package com.antique.service;

import com.antique.entity.Antique;
import com.antique.vo.AntiqueCardVO;
import com.antique.vo.AntiqueDetailVO;
import com.antique.vo.PageResultVO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 藏品服务接口 — 藏品列表/详情/搜索
 *
 * <p>继承 {@code IService<Antique>}，自动获得全套 CRUD 方法
 * （save/getById/page 等，由实现类继承的 ServiceImpl 提供）。
 * <p>实现类：{@code AntiqueServiceImpl}
 */
public interface AntiqueService extends IService<Antique> {

    /**
     * 分页查询藏品列表（仅返回卡片展示字段）
     *
     * @param categoryId 分类 ID，可选
     * @param isHot      是否热门（0-否 1-是），可选
     * @param page       页码，缺省或小于 1 默认 1
     * @param size       每页数量，缺省或小于 1 默认 10，最大 50
     * @return 分页卡片列表
     */
    PageResultVO<AntiqueCardVO> pageList(Long categoryId, Integer isHot, Integer page, Integer size);

    /**
     * 获取藏品详情（浏览次数 +1；已登录时判断是否已收藏）
     *
     * @param id     藏品 ID
     * @param userId 当前用户 ID，未登录为 null
     * @return 藏品详情 VO
     */
    AntiqueDetailVO getDetail(Long id, Long userId);

    /**
     * 搜索藏品（按名称模糊匹配，返回卡片字段）
     *
     * @param keyword    搜索关键词（必填）
     * @param categoryId 分类 ID，可选
     * @param page       页码，缺省或小于 1 默认 1
     * @param size       每页数量，缺省或小于 1 默认 10，最大 50
     * @return 分页卡片列表
     */
    PageResultVO<AntiqueCardVO> search(String keyword, Long categoryId, Integer page, Integer size);
}
