package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.AntiqueListQueryDTO;
import com.antique.dto.AntiqueSearchQueryDTO;
import com.antique.result.Result;
import com.antique.service.AntiqueService;
import com.antique.vo.AntiqueCardVO;
import com.antique.vo.AntiqueDetailVO;
import com.antique.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 藏品控制器 — 藏品列表、详情、搜索（公开接口）
 *
 * <p>路径前缀: /api/antique（公开访问，Token 拦截器按"可选认证"处理：
 * 有 Token 则校验并写入用户上下文，无 Token 则匿名放行）
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>获取藏品列表</td><td>GET /api/antique/list</td><td>🔓</td></tr>
 *   <tr><td>获取藏品详情</td><td>GET /api/antique/{id}</td><td>🔓</td></tr>
 *   <tr><td>搜索藏品</td><td>GET /api/antique/search</td><td>🔓</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/antique")
@RequiredArgsConstructor
@Tag(name = "藏品模块", description = "藏品列表、详情、搜索")
public class AntiqueController {

    private final AntiqueService antiqueService;

    /**
     * 接口 1：获取藏品列表（首页推荐/分类筛选，仅返回卡片展示字段）
     *
     * @param dto 查询参数：categoryId/isHot 可选，page 缺省 1，size 缺省 10 最大 50
     */
    @Operation(summary = "获取藏品列表")
    @GetMapping("/list")
    public Result<PageResultVO<AntiqueCardVO>> list(@Valid AntiqueListQueryDTO dto) {
        log.info("藏品列表请求: {}", dto);
        PageResultVO<AntiqueCardVO> data = antiqueService.pageList(
                dto.getCategoryId(), dto.getIsHot(), dto.getPage(), dto.getSize());
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 2：获取藏品详情（浏览次数 +1，登录用户额外返回 isFavorited）
     *
     * @param id 藏品 ID
     */
    @Operation(summary = "获取藏品详情")
    @GetMapping("/{id}")
    public Result<AntiqueDetailVO> detail(@PathVariable Long id) {
        // 可选认证：登录用户能拿到 userId（判 isFavorited），匿名用户为 null
        Long userId = UserContext.getUserId();
        log.info("藏品详情请求: id={}, userId={}", id, userId);
        AntiqueDetailVO data = antiqueService.getDetail(id, userId);
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 3：搜索藏品（按名称模糊匹配，返回卡片字段）
     *
     * @param dto 查询参数：keyword 必填，categoryId 可选，page/size 同列表接口
     */
    @Operation(summary = "搜索藏品")
    @GetMapping("/search")
    public Result<PageResultVO<AntiqueCardVO>> search(@Valid AntiqueSearchQueryDTO dto) {
        log.info("藏品搜索请求: {}", dto);
        PageResultVO<AntiqueCardVO> data = antiqueService.search(
                dto.getKeyword(), dto.getCategoryId(), dto.getPage(), dto.getSize());
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }
}
