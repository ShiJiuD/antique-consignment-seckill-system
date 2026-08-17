package com.antique.service.impl;

import com.antique.constant.MessageConstant;
import com.antique.exception.AuthException;
import com.antique.service.AiService;
import com.antique.vo.AiMessageVO;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手服务实现 — 通过 HTTP 调用 Python AI 服务（ai-side）
 *
 * <p>链路：前端 → AiController → 本类 → Python FastAPI（127.0.0.1:8001）→ 通义千问
 *
 * <h3>错误兜底</h3>
 * Python 服务未启动、超时（60 秒）、返回异常时统一抛出
 * {@link com.antique.exception.AuthException}（"AI服务暂时不可用"），由 GlobalExceptionHandler
 * 转换为 {@code Result.error(msg)} 返回前端，不暴露 500。
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /** Python AI 服务基础地址（application.yml 的 ai.python.url 配置） */
    @Value("${ai.python.url:http://127.0.0.1:8001}")
    private String pythonUrl;

    /** HTTP 客户端（连接超时 5s，读取超时 60s — LLM 生成较慢） */
    private RestClient restClient;

    /**
     * 初始化 HTTP 客户端
     *
     * <p>RestClient 是 Spring 6.1+ 内置的 HTTP 客户端（无需额外依赖），
     * 使用 SimpleClientHttpRequestFactory 配置连接/读取超时。
     */
    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 连接超时 5 秒
        factory.setReadTimeout(60000);     // 读取超时 60 秒（通义千问生成较慢）
        this.restClient = RestClient.builder()
                .baseUrl(pythonUrl)
                .requestFactory(factory)
                .build();
        log.info("AI 服务已初始化, Python 地址: {}", pythonUrl);
    }

    /**
     * AI 问答：调用 Python 服务 POST /chat
     *
     * <p>Python 侧执行完整 RAG 流程（知识库检索 + 历史 + 通义千问生成），
     * 并将本轮问答保存到该用户的本地历史文件。
     */
    @Override
    public String chat(Long userId, String question) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("question", question);

            JsonNode resp = restClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            // 校验响应结构：{code:1=成功, data:{answer}}
            if (resp == null || resp.get("code") == null || resp.get("code").asInt() != 1) {
                log.warn("Python AI 服务返回异常: userId={}, resp={}", userId, resp);
                throw new AuthException(MessageConstant.AI_SERVICE_UNAVAILABLE);
            }
            return resp.path("data").path("answer").asText();
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            // Python 服务未启动 / 超时 / 网络异常等 → 兜底提示
            log.error("调用 Python AI 服务失败: userId={}, question={}", userId, question, e);
            throw new AuthException(MessageConstant.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 获取对话历史：调用 Python 服务 GET /history?userId=
     *
     * <p>Python 侧读取该用户的历史文件并返回最近 20 条（时间正序）。
     */
    @Override
    public List<AiMessageVO> history(Long userId) {
        try {
            JsonNode resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/history")
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (resp == null || resp.get("code") == null || resp.get("code").asInt() != 1) {
                log.warn("Python AI 服务历史接口异常: userId={}, resp={}", userId, resp);
                throw new AuthException(MessageConstant.AI_SERVICE_UNAVAILABLE);
            }

            // 将 Python 返回的 [{role, content}, ...] 转换为 VO 列表
            List<AiMessageVO> list = new ArrayList<>();
            JsonNode items = resp.path("data").path("list");
            for (JsonNode item : items) {
                list.add(new AiMessageVO(
                        item.path("role").asText(),
                        item.path("content").asText()));
            }
            return list;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用 Python AI 服务历史接口失败: userId={}", userId, e);
            throw new AuthException(MessageConstant.AI_SERVICE_UNAVAILABLE);
        }
    }
}
