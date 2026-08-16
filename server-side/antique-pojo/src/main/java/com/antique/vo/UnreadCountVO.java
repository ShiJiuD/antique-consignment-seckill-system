package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 未读消息数返回 VO — 前端角标使用
 *
 * <p>包装成对象而不是直接返回数字，是为了统一接口格式：
 * {@code { "code":1, "data":{"count":3} }}，前端按 data 对象统一处理。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnreadCountVO implements Serializable {

    /** 未读消息总数（前端 TabBar 的红色角标） */
    private Integer count;
}
