package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.FavoriteDTO;
import com.antique.dto.FavoritePageQueryDTO;
import com.antique.result.Result;
import com.antique.service.FavoriteService;
import com.antique.vo.AntiqueCardVO;
import com.antique.vo.PageResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏控制器 — 收藏、取消收藏、我的收藏列表（需认证）
 *
 * <p>路径前缀: /api/favorite（需认证，Token 校验由 TokenInterceptor 完成）
 * <p>当前用户 ID 从 {@code UserContext.getUserId()} 获取，无法伪造。
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>收藏藏品</td><td>POST /api/favorite/add</td><td>🔒</td></tr>
 *   <tr><td>取消收藏</td><td>POST /api/favorite/remove</td><td>🔒</td></tr>
 *   <tr><td>获取我的收藏列表</td><td>GET /api/favorite/list</td><td>🔒</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@Tag(name = "收藏模块", description = "收藏、取消收藏、我的收藏列表")
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 接口 4：收藏藏品（校验在售、防重复收藏，成功后收藏数 +1）
     *
     * @param dto 藏品 ID
     */
    @Operation(summary = "收藏藏品")
    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody FavoriteDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("收藏请求: userId={}, antiqueId={}", userId, dto.getAntiqueId());
        favoriteService.add(userId, dto.getAntiqueId());
        return Result.success(null, MessageConstant.FAVORITE_ADD_SUCCESS);
    }

    /**
     * 接口 5：取消收藏（未收藏过返回 code=0，成功后收藏数 -1）
     *
     * @param dto 藏品 ID
     */
    @Operation(summary = "取消收藏")
    @PostMapping("/remove")
    public Result<?> remove(@Valid @RequestBody FavoriteDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("取消收藏请求: userId={}, antiqueId={}", userId, dto.getAntiqueId());
        favoriteService.remove(userId, dto.getAntiqueId());
        return Result.success(null, MessageConstant.FAVORITE_REMOVE_SUCCESS);
    }

    /**
     * 接口 6：获取我的收藏列表（仅卡片字段，已下架/已删除不展示）
     *
     * @param dto 分页参数：page 缺省 1，size 缺省 10 最大 50
     */
    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/list")
    public Result<PageResultVO<AntiqueCardVO>> list(@Valid FavoritePageQueryDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("我的收藏列表请求: userId={}, {}", userId, dto);
        PageResultVO<AntiqueCardVO> data = favoriteService.pageFavorites(userId, dto.getPage(), dto.getSize());
        return Result.success(data, MessageConstant.QUERY_SUCCESS);
    }
}
