package com.antique.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 我的收藏分页查询参数
 *
 * <p>接口：GET /api/favorite/list
 *
 * <h3>参数说明</h3>
 * <ul>
 *   <li>page/size 均可缺省，缺省时由 Service 层归一化为默认值（1/10）</li>
 *   <li>page/size 越界（<1 或 >50）由 @Min/@Max 校验拦截，返回错误提示</li>
 * </ul>
 */
@Data
public class FavoritePageQueryDTO {

    /** 页码，缺省默认 1，不能小于 1 */
    @Min(value = 1, message = "页码不能小于1")
    private Integer page;

    /** 每页数量，缺省默认 10，范围 1-50 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 50, message = "每页数量不能大于50")
    private Integer size;
}
