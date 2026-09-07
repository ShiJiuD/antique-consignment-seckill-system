package com.antique.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果 VO — 所有分页接口的统一返回结构
 *
 * <pre>{@code
 * {
 *   "list":  [...],   // 当前页数据列表
 *   "total": 100,     // 总记录数
 *   "page":  1,       // 当前页码
 *   "size":  10       // 每页数量
 * }
 * }</pre>
 *
 * @param <T> 列表元素类型（如 {@link AntiqueCardVO}）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResultVO<T> implements Serializable {

    /** 当前页数据列表 */
    private List<T> list;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer page;

    /** 每页数量 */
    private Integer size;
}
