package com.antique.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 藏品列表查询参数
 *
 * <p>接口：GET /api/antique/list
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>categoryId/isHot 均可缺省：不传返回全部在售藏品</li>
 *   <li>page/size 缺省时由 Service 层归一化为默认值（1/10）</li>
 *   <li>page/size 越界（<1 或 >50）由 @Min/@Max 校验拦截，返回错误提示</li>
 * </ul>
 */
@Data
public class AntiqueListQueryDTO {

    /** 页码，缺省默认 1，不能小于 1 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page;

    /** 每页数量，缺省默认 10，范围 1-50 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能大于50")
    private Integer size;

    /** 分类 ID（可选：1-瓷器 2-字画 3-玉器 4-铜器 5-杂项） */
    private Long categoryId;

    /** 是否热门（可选：0-否 1-是） */
    private Integer isHot;
}
