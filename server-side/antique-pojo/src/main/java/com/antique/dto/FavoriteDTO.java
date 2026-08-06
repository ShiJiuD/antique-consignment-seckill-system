package com.antique.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收藏操作 — 请求体
 *
 * <p>接口：POST /api/favorite/add、POST /api/favorite/remove
 *
 * <h3>校验规则</h3>
 * <ul>
 *   <li>antiqueId 不能为空</li>
 * </ul>
 */
@Data
public class FavoriteDTO {

    /** 藏品 ID，必填 */
    @NotNull(message = "藏品ID不能为空")
    private Long antiqueId;
}
