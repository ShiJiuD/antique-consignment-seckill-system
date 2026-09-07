package com.antique.dto;

import lombok.Data;

/**
 * 修改个人信息 — 请求体
 *
 * <p>接口：PUT /api/user/profile
 * <p>全部字段可选，只更新传入的非 null 字段。
 * <p>手机号不支持通过此接口修改。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>nickname: 新昵称（可选）</li>
 *   <li>avatar: 新头像 URL（可选）</li>
 * </ul>
 */
@Data
public class UpdateProfileDTO {

    /** 新昵称（可选） */
    private String nickname;

    /** 新头像 URL（可选） */
    private String avatar;
}
