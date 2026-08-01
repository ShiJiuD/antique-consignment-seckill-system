package com.antique.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * <p>所有接口统一返回此格式的 JSON：
 * <pre>{@code
 * { "code": 1, "msg": "提示信息", "data": {...} }
 * }</pre>
 *
 * @param <T> 业务数据类型
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码：1=成功，0=失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 业务数据，无数据时返回 null */
    private T data;

    // ==================== 成功响应 ====================

    /**
     * 操作成功（无数据）
     * <p>返回 {code:1, msg:"操作成功", data:null}
     */
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "操作成功";
        return result;
    }

    /**
     * 操作成功（带数据）
     * <p>返回 {code:1, msg:"操作成功", data: data}
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "操作成功";
        result.data = data;
        return result;
    }

    /**
     * 操作成功（带数据和自定义消息）
     * <p>返回 {code:1, msg: msg, data: data}
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = msg;
        result.data = data;
        return result;
    }

    // ==================== 失败响应 ====================

    /**
     * 操作失败
     * <p>返回 {code:0, msg: msg, data:null}
     *
     * @param msg 错误提示信息
     */
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
