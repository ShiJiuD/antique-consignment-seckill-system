package com.antique.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.antique.constant.RedisConstant.KEY_TOKEN;

/**
 * WebSocket 握手拦截器 — 通过 query 参数 Token 校验身份
 *
 * <p>浏览器 new WebSocket(url) 无法自定义请求头，所以前端把登录返回的 token 拼在 URL 上：
 * {@code ws://host:port/api/ws?token=xxx}，这里负责校验它，通过才允许建立连接。
 *
 * <p>校验逻辑与 HTTP 的 TokenInterceptor 完全一致（同一把 Redis 钥匙），
 * 只是 token 来源从请求头换成了 URL 参数。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WsTokenHandshakeInterceptor implements HandshakeInterceptor {

    /** 握手 attributes 中存放 userId 的 key（后面的处理器用它取"这条连接是谁的"） */
    public static final String ATTR_USER_ID = "userId";

    // Spring 自动注入的 Redis 操作对象（不用自己 new）
    private final StringRedisTemplate redisTemplate;

    /**
     * 握手前执行：校验通过返回 true 放行，失败返回 false 拒绝连接
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {

        // 步骤 1：从 URL 的 query 参数里取 token（?token=xxx）
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().getFirst("token");

        // 没带 token 或 token 是空串 → 直接拒绝（401）
        if (token == null || token.isBlank()) {
            log.warn("WebSocket 握手缺少 token: uri={}", request.getURI());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 步骤 2：查 Redis「antique:token:{token}」——和 HTTP 登录态校验同一份数据
        String userJson;
        try {
            userJson = redisTemplate.opsForValue().get(KEY_TOKEN + token);
        } catch (Exception e) {
            // Redis 挂掉时不能放行也不能崩，按"校验失败"处理
            log.warn("WebSocket 握手 Redis 校验失败: token={}", token, e);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Redis 里查不到 = token 过期/登出/伪造 → 拒绝
        if (userJson == null) {
            log.warn("WebSocket 握手 token 无效或已过期");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // 步骤 3：解析出 userId，放进 attributes（Map）——处理器建立连接时从这里取
        JSONObject userJsonObj = JSONUtil.parseObj(userJson);
        Long userId = userJsonObj.getLong("userId");
        attributes.put(ATTR_USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后无需处理（接口要求实现，留空）
    }
}
