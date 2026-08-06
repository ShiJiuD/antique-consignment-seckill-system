package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.dto.PasswordLoginDTO;
import com.antique.dto.SendCodeDTO;
import com.antique.dto.SmsLoginDTO;
import com.antique.result.Result;
import com.antique.service.UserService;
import com.antique.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — 处理登录和验证码相关请求
 *
 * <p>路径前缀: /api/auth（在 WebMvcConfiguration 中配置为无需认证放行）
 *
 * <h3>接口列表</h3>
 * <table>
 *   <tr><th>接口</th><th>路径</th><th>认证</th></tr>
 *   <tr><td>发送短信验证码</td><td>POST /api/auth/sms/send</td><td>🔓</td></tr>
 *   <tr><td>短信验证码登录</td><td>POST /api/auth/login/sms</td><td>🔓</td></tr>
 *   <tr><td>密码登录</td><td>POST /api/auth/login/password</td><td>🔓</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证模块", description = "短信验证码发送、短信登录、密码登录")
public class AuthController {

    private final UserService userService;

    /**
     * 接口 1：发送短信验证码
     *
     * <p>POST /api/auth/sms/send
     * <p>验证码恒为 666666，存入 Redis TTL 5 分钟。
     * 不调用真实短信服务商。
     *
     * @param dto 包含手机号（格式 ^1[3-9]\d{9}$）
     * @return {"code":1,"msg":"验证码发送成功","data":null}
     */
    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms/send")
    public Result<?> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        log.info("发送验证码: phone={}", dto.getPhone());
        userService.sendSmsCode(dto.getPhone());
        return Result.success(null, MessageConstant.CODE_SEND_SUCCESS);
    }

    /**
     * 接口 2：短信验证码登录（含自动注册）
     *
     * <p>POST /api/auth/login/sms
     * <p>校验验证码后，若用户不存在则自动创建（nickname="藏友"+随机6位数字）。
     * 登录成功后返回 32 位 Token + 用户信息。
     *
     * @param dto 包含手机号和 6 位验证码
     * @return {"code":1,"msg":"登录成功","data":{"token":"...","userInfo":{...}}}
     */
    @Operation(summary = "短信验证码登录")
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        log.info("短信登录: phone={}", dto.getPhone());
        LoginVO vo = userService.smsLogin(dto.getPhone(), dto.getCode());
        log.info("短信登录成功: userId={}", vo.getUserInfo().getId());
        return Result.success(vo, MessageConstant.LOGIN_SUCCESS);
    }

    /**
     * 接口 3：密码登录
     *
     * <p>POST /api/auth/login/password
     * <p>手机号 + 密码登录，BCrypt 校验密码。
     * 用户不存在和密码错误返回统一提示（防撞库）。
     *
     * @param dto 包含手机号和明文密码
     * @return {"code":1,"msg":"登录成功","data":{"token":"...","userInfo":{...}}}
     */
    @Operation(summary = "密码登录")
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto) {
        log.info("密码登录: phone={}", dto.getPhone());
        LoginVO vo = userService.passwordLogin(dto.getPhone(), dto.getPassword());
        log.info("密码登录成功: userId={}", vo.getUserInfo().getId());
        return Result.success(vo, MessageConstant.LOGIN_SUCCESS);
    }
}
