package com.antique.interceptor;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.antique.constant.RedisConstant.KEY_TOKEN;
import static com.antique.constant.RedisConstant.TOKEN_TTL;

/**
 * Token 认证拦截器 — Redis 随机 Token 方案
 *
 * <h3>完整认证流程</h3>
 * <ol>
 *   <li>从请求头提取 {@code Authorization: Bearer <token>}</li>
 *   <li>根据 Token 从 Redis 查询用户信息（Key: antique:token:{token}）</li>
 *   <li>若 Token 不存在或 Redis 不可达 → 返回 401 {"code":0,"msg":"未登录，请先登录"}</li>
 *   <li>解析 Redis 中的 JSON → 获取 userId 和 phone</li>
 *   <li>设置 {@code UserContext}（ThreadLocal）</li>
 *   <li>刷新 Token 在 Redis 中的 TTL 为 7 天（续期）</li>
 *   <li>请求处理完成后 → afterCompletion 清理 {@code UserContext}</li>
 * </ol>
 *
 * <h3>与 ciTY Art 的核心差异</h3>
 * <ul>
 *   <li>ciTY Art 使用 JWT 自包含 Token + Redis 黑名单</li>
 *   <li>本项目使用 Redis 存储 Token，Token 本身是随机 UUID 不含用户信息</li>
 *   <li>优势：可随时在 Redis 中删除 Token 使其立即失效，无需等 JWT 过期</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 请求前拦截 — Token 校验、续期、用户上下文设置
     *
     * @return true=放行, false=拦截并返回 401 JSON
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // ====== 步骤 1：提取 Token ======
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":0,\"msg\":\"" + MessageConstant.NOT_LOGIN + "\",\"data\":null}");
            return false;
        }

        // 截取 "Bearer " 之后的 Token 字符串
        String token = authHeader.substring(7);
        String redisKey = KEY_TOKEN + token;

        // ====== 步骤 2：从 Redis 获取用户信息 ======
        String userJson;
        try {
            userJson = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("Redis 读取 Token 失败: token={}", token, e);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"系统繁忙\",\"data\":null}");
            return false;
        }

        // Token 不存在或已过期
        if (userJson == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":0,\"msg\":\"" + MessageConstant.NOT_LOGIN + "\",\"data\":null}");
            return false;
        }

        // ====== 步骤 3：解析用户信息 JSON ======
        // TypeReference 提供泛型类型信息，避免编译器"unchecked cast"警告
        Map<String, Object> userMap = MAPPER.readValue(userJson, new TypeReference<Map<String, Object>>() {});
        Long userId = Long.valueOf(userMap.get("userId").toString());
        String phone = (String) userMap.get("phone");

        // ====== 步骤 4：设置用户上下文（ThreadLocal） ======
        UserContext.set(userId, phone);

        // ====== 步骤 5：Token 续期 7 天 ======
        // 活跃用户无需频繁登录，每次请求自动刷新 TTL
        try {
            redisTemplate.expire(redisKey, TOKEN_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 续期失败不阻塞业务，仅记录日志
            log.warn("Redis Token 续期失败: token={}, error={}", token, e.getMessage());
        }

        return true;
    }

    /**
     * 请求完成后清理 — 防止 ThreadLocal 内存泄漏
     *
     * <p>无论请求成功或异常，都会执行此方法清理线程上下文。
     * Tomcat 线程池会复用线程，不清理会导致下次请求读到上一个用户的数据。
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
