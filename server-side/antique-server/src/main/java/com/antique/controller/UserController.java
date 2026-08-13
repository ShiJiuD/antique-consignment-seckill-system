package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.UpdateProfileDTO;
import com.antique.result.Result;
import com.antique.service.UserService;
import com.antique.service.UserSignService;
import com.antique.vo.SignVO;
import com.antique.vo.UserCenterVO;
import com.antique.vo.UserProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器 — 处理个人信息和退出登录
 *
 * <p>路径前缀: /api/user（需认证，Token 校验由 TokenInterceptor 完成）
 * <p>当前用户 ID 从 {@code UserContext.getUserId()} 获取，无法伪造。
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>获取个人信息</td><td>GET /api/user/profile</td><td>🔒</td></tr>
 *   <tr><td>修改个人信息</td><td>PUT /api/user/profile</td><td>🔒</td></tr>
 *   <tr><td>退出登录</td><td>POST /api/user/logout</td><td>🔒</td></tr>
 *   <tr><td>个人中心初始化</td><td>GET /api/user/center/info</td><td>🔒</td></tr>
 *   <tr><td>执行签到</td><td>POST /api/user/sign</td><td>🔒</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "个人信息查询与修改、退出登录、签到")
public class UserController {

    private final UserService userService;
    private final UserSignService userSignService;

    /**
     * 接口 4：获取个人信息
     *
     * <p>GET /api/user/profile
     * <p>页面刷新/重新打开时调用，根据 Token 返回最新用户信息。
     * <p>手机号脱敏返回（138****8000）。
     *
     * @return 用户个人信息 VO
     */
    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Long userId = UserContext.getUserId();
        log.info("获取个人信息: userId={}", userId);
        UserProfileVO vo = userService.getProfile(userId);
        return Result.success(vo);
    }

    /**
     * 接口 5：修改个人信息
     *
     * <p>PUT /api/user/profile
     * <p>支持修改昵称和头像，全部字段可选（只更新非 null 字段）。
     * <p>手机号不支持通过此接口修改。
     *
     * @param dto 包含新昵称(可选)和新头像URL(可选)
     * @return "修改成功"
     */
    @Operation(summary = "修改个人信息")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        log.info("修改个人信息: userId={}, nickname={}, avatar={}", userId, dto.getNickname(), dto.getAvatar());
        userService.updateProfile(userId, dto.getNickname(), dto.getAvatar());
        return Result.success(null, MessageConstant.PROFILE_UPDATE_SUCCESS);
    }

    /**
     * 接口 6：退出登录
     *
     * <p>POST /api/user/logout
     * <p>从 Header 提取 Token，删除 Redis 中的对应 Key，Token 立即失效。
     * <p>即使 Redis 删除失败也不抛出异常（Token 最终会在 7 天后自然过期）。
     *
     * @param request HTTP 请求（用于提取 Authorization Header）
     * @return "退出成功"
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("退出登录: token={}...", token.substring(0, 8));
            userService.removeToken(token);
        }
        return Result.success(null, MessageConstant.LOGOUT_SUCCESS);
    }

    /**
     * 接口 7：个人中心初始化
     *
     * <p>GET /api/user/center/info
     * <p>进入"我的"页面时调用，返回用户信息 + 签到信息 + 统计信息。
     * <p>连续签到天数通过 user_sign_record 流水表回推（最后签到日期与今天/昨天对比）。
     *
     * @return 个人中心响应 VO
     */
    @Operation(summary = "个人中心初始化")
    @GetMapping("/center/info")
    public Result<UserCenterVO> getCenterInfo() {
        Long userId = UserContext.getUserId();
        log.info("个人中心初始化: userId={}", userId);
        UserCenterVO vo = userSignService.getCenterInfo(userId);
        return Result.success(vo, MessageConstant.QUERY_SUCCESS);
    }

    /**
     * 接口 8：执行签到
     *
     * <p>POST /api/user/sign
     * <p>点击"签到"按钮时调用。防重双保险：Redis BitMap 极速防重 +
     * 数据库唯一索引 uk_user_date 物理兜底（并发/Redis 不可用场景）。
     * <p>连续判定：昨天签过 → 连续 +1，断签 → 重置为 1。
     *
     * @return 签到响应 VO（最新积分 + 本次奖励）
     */
    @Operation(summary = "执行签到")
    @PostMapping("/sign")
    public Result<SignVO> sign() {
        Long userId = UserContext.getUserId();
        log.info("执行签到: userId={}", userId);
        SignVO vo = userSignService.sign(userId);
        return Result.success(vo, String.format(MessageConstant.SIGN_SUCCESS, vo.getTodayReward()));
    }
}
