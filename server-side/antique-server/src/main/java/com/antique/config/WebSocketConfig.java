package com.antique.config;

import com.antique.websocket.MessageWebSocketHandler;
import com.antique.websocket.WsTokenHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 — 把推送端点注册进 WebSocket 框架
 *
 * <p>为什么实现 WebSocketConfigurer 接口？这是 WebSocket 框架留的定制槽位：
 * Spring 启动时找到实现它的类，回调 registerWebSocketHandlers，把注册器递给我们。
 * （对应 HTTP 那边是 WebMvcConfigurer，两个框架各留各的槽位。）
 *
 * <p>端点 /api/ws 带 /api/ 前缀遵循接口路径规范；因为浏览器无法给 WebSocket 请求
 * 自定义请求头，它不走 HTTP 的 TokenInterceptor（已在 WebMvcConfiguration 排除），
 * token 校验由握手拦截器自己完成。
 */
@Configuration
@EnableWebSocket  // 开关：不写这个注解，下面注册的端点不生效
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    /** 端点路径（WebMvcConfiguration 排除拦截器也引用它，一处定义两处用） */
    public static final String WS_ENDPOINT = "/api/ws";

    // 从容器注入处理器和握手拦截器
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final WsTokenHandshakeInterceptor wsTokenHandshakeInterceptor;

    /**
     * 框架启动时回调 —— 把"处理器 + 拦截器 + 路径"登记进 WebSocket 框架
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(messageWebSocketHandler, WS_ENDPOINT)   // 连 /api/ws 的连接交给处理器
                .addInterceptors(wsTokenHandshakeInterceptor)       // 每次连接先过握手拦截器（token 校验）
                .setAllowedOriginPatterns("*");                     // 开发环境放开跨域（生产应限制域名）
    }
}
