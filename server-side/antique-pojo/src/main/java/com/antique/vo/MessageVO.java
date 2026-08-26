package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 消息返回 VO — 消息列表接口的元素结构
 *
 * <p>为什么不直接返回 Message 实体？实体里 userId 等内部字段不该暴露给前端，
 * VO 专门定义"前端要什么就返回什么"，是返回层的封装惯例。
 */
// @Builder：链式构造（MessageVO.builder().id(1).title("x").build()）；无参/全参构造配合 JSON 框架使用
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVO implements Serializable {

    /** 消息 ID（标记已读时用） */
    private Long id;

    /** 消息类型：1-系统通知，2-订单消息，3-AI助手（预留） */
    private Integer type;

    /** 标题（前端可按标题分组展示） */
    private String title;

    /** 消息内容 */
    private String content;

    /** 是否已读：0-未读，1-已读（前端据此显示红点/加粗） */
    private Integer isRead;

    /** 创建时间，后端已格式化成 "2026-08-16 10:30:00"，前端直接显示 */
    private String createdTime;
}
