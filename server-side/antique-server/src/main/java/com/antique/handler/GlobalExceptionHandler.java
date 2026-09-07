package com.antique.handler;

import com.antique.constant.MessageConstant;
import com.antique.exception.BaseException;
import com.antique.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 统一将异常转换为 Result 响应
 *
 * <h3>异常处理优先级（由具体到笼统）</h3>
 * <ol>
 *   <li>{@code BaseException} → 业务异常，返回自定义消息</li>
 *   <li>{@code BindException} → 参数绑定/校验失败（@Valid），返回第一条错误</li>
 *   <li>{@code HttpMessageNotReadableException} → 请求体格式错误（JSON 解析失败）</li>
 *   <li>{@code Exception} → 兜底，返回"系统异常"，记录完整堆栈</li>
 * </ol>
 *
 * <h3>设计说明</h3>
 * <p>采用 {@code @RestControllerAdvice} 而非在每个 Controller 中 try-catch，
 * 使得 Controller 代码只关注正常业务逻辑，异常处理完全解耦。
 * 这个模式直接参考了 ciTY Art 项目。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常 — 直接返回异常中的消息给前端
     *
     * <p>例如：throw new AuthException("验证码错误或已过期")
     * → {"code":0,"msg":"验证码错误或已过期","data":null}
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 参数绑定/校验异常 — @Valid 校验失败时触发
     *
     * <p>覆盖两类场景（{@code MethodArgumentNotValidException} 继承自
     * {@code BindException}，一个 handler 即可全部处理）：
     * <ul>
     *   <li>@RequestBody 请求体校验失败 → 如 POST /api/favorite/add</li>
     *   <li>@ModelAttribute 查询参数 DTO 校验失败 → 如 GET /api/antique/search</li>
     * </ul>
     * 从所有字段错误中取第一条返回，避免同时暴露过多字段信息。
     * 例如：keyword 为空 → {"code":0,"msg":"搜索关键词不能为空","data":null}
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse(MessageConstant.PARAM_ERROR);
        return Result.error(msg);
    }

    /**
     * 请求体解析异常 — JSON 格式错误或字段类型不匹配
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return Result.error(MessageConstant.PARAM_FORMAT_ERROR);
    }

    /**
     * 兜底异常 — 所有未预期异常的统一处理
     *
     * <p>记录完整堆栈日志供排查，向前端返回通用错误消息而非内部细节。
     * 避免将数据库表结构、堆栈信息等敏感数据暴露给客户端。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }
}
