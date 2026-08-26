package com.antique.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 藏品搜索查询参数
 *
 * <p>接口：GET /api/antique/search
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>keyword 必填且不能为空白（校验失败由全局异常处理器统一返回提示）</li>
 *   <li>categoryId 可选，传了则限定在分类内搜索</li>
 *   <li>page/size 缺省时由 Service 层归一化为默认值（1/10），越界由 @Min/@Max 拦截</li>
 * </ul>
 */
@Data
public class AntiqueSearchQueryDTO {

    /** 搜索关键词，必填（按名称模糊匹配） */
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    /** 分类 ID（可选，在分类内搜索） */
    private Long categoryId;

    /** 页码，缺省默认 1，不能小于 1 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page;

    /** 每页数量，缺省默认 10，范围 1-50 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能大于50")
    private Integer size;
}
