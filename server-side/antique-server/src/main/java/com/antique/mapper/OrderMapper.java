package com.antique.mapper;

import com.antique.entity.Orders;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 订单主表 Mapper 接口
 *
 * <p>继承 BaseMapper 自动获得单表 CRUD 方法（selectById/selectPage/insert 等）。
 * 状态流转使用 @Update 条件更新（WHERE 带当前状态），并发下保证幂等：
 * 影响行数为 0 表示状态已被其他事务修改，无需重复处理。
 */
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 订单支付（待付款 → 待发货）
     *
     * <p>{@code AND status = 0} 条件更新：并发重复点击时仅第一次生效，
     * 第二次影响行数 0（幂等）。
     *
     * @param orderId 订单 ID
     * @return 受影响行数，0 表示订单已非待付款状态
     */
    @Update("UPDATE orders SET status = 1, pay_time = NOW() " +
            "WHERE id = #{orderId} AND status = 0")
    int payOrder(Long orderId);

    /**
     * 惰性批量取消超时订单（用户维度，查询订单列表前调用）
     *
     * <p>与延迟消息/定时任务并发时，条件更新保证同一订单只会被关单一次。
     * 超时判定使用数据库时间 NOW()（权威），避免多实例应用时钟偏差。
     *
     * @param userId 买家用户 ID
     * @return 受影响行数（本次实际取消的订单数）
     */
    @Update("UPDATE orders SET status = 4, cancel_type = 2, cancel_time = NOW() " +
            "WHERE user_id = #{userId} AND status = 0 AND pay_deadline < NOW()")
    int closeOverdueByUser(Long userId);

    /**
     * 超时订单扫描（定时任务兜底，keyset 分页）
     *
     * <p>{@code id > lastId} 游标分页而非 LIMIT offset：offset 增大后性能不劣化。
     * 配合联合索引 (status, pay_deadline)。
     *
     * @param lastId    上一批最后一条订单 ID（首轮传 0）
     * @param batchSize 每批数量（如 100）
     * @return 本批超时订单 ID 列表（升序）
     */
    @Select("SELECT id FROM orders " +
            "WHERE status = 0 AND pay_deadline < NOW() AND id > #{lastId} " +
            "ORDER BY id ASC LIMIT #{batchSize}")
    List<Long> selectOverdueIds(@Param("lastId") Long lastId, @Param("batchSize") int batchSize);

    /**
     * 防重复下单校验（一物一单）
     *
     * <p>同一藏品存在任意未完成订单（待付款/待发货/待收货）时不允许再次下单。
     * 自定义 SQL 需自行拼接 deleted_time 条件（MyBatis-Plus 逻辑删除不作用于 @Select）。
     *
     * @param antiqueId 藏品 ID
     * @return 未完成订单数（>0 即重复下单）
     */
    @Select("SELECT COUNT(*) FROM order_items i " +
            "INNER JOIN orders o ON o.id = i.order_id " +
            "WHERE i.antique_id = #{antiqueId} AND o.status IN (0, 1, 2) AND o.deleted_time IS NULL")
    long countActiveByAntiqueId(Long antiqueId);
}
