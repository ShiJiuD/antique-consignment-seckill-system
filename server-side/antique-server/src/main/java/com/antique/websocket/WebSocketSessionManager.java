package com.antique.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话管理器 — "谁在线"的登记簿
 *
 * <p>结构：userId → 该用户的会话集合（Set 支持同一账号多端登录，每端一个连接）。
 * 用 ConcurrentHashMap 是因为多个请求线程会同时读写它，必须线程安全。
 *
 * <p>登记簿的作用：
 * <ul>
 *   <li>连接建立 → add：记一笔"这个用户在线"</li>
 *   <li>连接断开/异常 → remove：划掉，防止僵尸连接</li>
 *   <li>下单成功 → sendToUser：查登记簿，给该用户所有在线连接发消息</li>
 * </ul>
 */
@Slf4j
@Component
public class WebSocketSessionManager {

    // 整个应用只有这一份登记簿（static）；外层 key=userId，内层 = 这个人的所有连接
    private static final Map<Long, Set<WebSocketSession>> SESSIONS = new ConcurrentHashMap<>();

    /**
     * 连接建立时登记（用户上线）
     */
    public void add(Long userId, WebSocketSession session) {
        // 没有这个 userId 就先建一个空集合（computeIfAbsent：有则复用，无则创建）
        SESSIONS.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    /**
     * 连接断开/异常时划掉登记，防止"人走了登记还在"
     */
    public void remove(WebSocketSession session) {
        // 从连接上取握手时存的 userId
        Object userIdObj = session.getAttributes().get(WsTokenHandshakeInterceptor.ATTR_USER_ID);
        if (!(userIdObj instanceof Long userId)) {
            return;
        }
        Set<WebSocketSession> sessions = SESSIONS.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        // 这个用户已经没有任何在线连接了 → 整个键移除，防止 Map 越攒越大
        if (sessions.isEmpty()) {
            SESSIONS.remove(userId);
        }
    }

    /**
     * 给指定用户的所有在线连接推送文本消息
     *
     * <p>用户不在线就静默跳过——消息已落库，前端进消息页/轮询未读数兜底，不丢。
     */
    public void sendToUser(Long userId, String payload) {
        Set<WebSocketSession> sessions = SESSIONS.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;  // 没人在线，直接返回
        }
        // 逐条连接发送：单条失败只记日志，不影响其他连接
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (Exception e) {
                log.warn("WebSocket 推送失败: userId={}, sessionId={}", userId, session.getId(), e);
            }
        }
    }
}
