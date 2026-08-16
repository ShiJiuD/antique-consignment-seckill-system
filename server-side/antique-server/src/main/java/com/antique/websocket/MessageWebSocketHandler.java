package com.antique.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket 消息处理器 — 连接生命周期管理
 *
 * <p>继承 Spring 写好的 TextWebSocketHandler（基类已实现连接管理框架），
 * 我们只需覆盖三个时机：连接建立（登记）、连接关闭/异常（清理）。
 * V1 为纯服务端推送，客户端发来的消息直接忽略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageWebSocketHandler extends TextWebSocketHandler {

    // 注入"在线登记簿"：本类的活就是把连接登记进去/划掉
    private final WebSocketSessionManager sessionManager;

    /**
     * 连接建立成功 → 登记"这个用户在线"（下单推送才能找到他）
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 取握手拦截器存进 attributes 的 userId，确定这条连接是谁的
        Long userId = (Long) session.getAttributes().get(WsTokenHandshakeInterceptor.ATTR_USER_ID);
        sessionManager.add(userId, session);
        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());
    }

    /**
     * 连接关闭 → 划掉登记（正常关闭和异常断开都要清理，否则会留下"假在线"）
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.remove(session);
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * 传输层异常（网络断开等）→ 同样清理登记
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessionManager.remove(session);
        log.warn("WebSocket 传输异常，会话已清理: sessionId={}", session.getId(), exception);
    }

    /**
     * 客户端发来文本消息 → V1 忽略（预留后续指令）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.debug("收到客户端消息（忽略）: sessionId={}, payload={}", session.getId(), message.getPayload());
    }
}
