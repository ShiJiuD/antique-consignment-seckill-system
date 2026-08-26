package com.antique.task;

import com.antique.mapper.OrderMapper;
import com.antique.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单超时定时任务（兜底）— 每分钟扫描超时待付款订单并关单
 *
 * <p>延迟消息为主通道，本任务兜底防消息丢失/消费失败：
 * <ul>
 *   <li>调度锁：多实例部署时同一时刻仅一个实例扫描（抢不到锁跳过本轮）</li>
 *   <li>keyset 分页：{@code id > lastId} 游标扫描，避免 LIMIT offset 深分页性能劣化</li>
 *   <li>联合索引：{@code (status, pay_deadline)} 支撑等值+范围查询</li>
 *   <li>幂等：逐单调用 closeOrderByTimeout（条件更新），与延迟消息/惰性检查并发安全</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final RedissonClient redissonClient;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /** 调度锁 Key（多实例互斥） */
    private static final String SCHED_LOCK_KEY = "lock:sched:order-close";

    /** 每批扫描数量 */
    private static final int BATCH_SIZE = 100;

    /** 抢锁等待时间：0 秒（抢不到直接跳过本轮，不排队） */
    private static final long LOCK_WAIT_SECONDS = 0;

    /** 锁持有时间：30 秒（单轮扫描应在 30s 内完成，超时自动释放防死锁） */
    private static final long LOCK_LEASE_SECONDS = 30;

    /**
     * 每分钟执行：扫描超时订单并关单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void closeOverdueOrders() {
        RLock lock = redissonClient.getLock(SCHED_LOCK_KEY);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                // 其他实例正在扫描，本轮跳过
                return;
            }

            // keyset 分页扫描：从 id=0 开始，每批取 BATCH_SIZE 条，直至本批不足
            Long lastId = 0L;
            int processed = 0;
            while (true) {
                List<Long> overdueIds = orderMapper.selectOverdueIds(lastId, BATCH_SIZE);
                if (overdueIds.isEmpty()) {
                    break;
                }
                for (Long orderId : overdueIds) {
                    orderService.closeOrderByTimeout(orderId);
                    processed++;
                }
                lastId = overdueIds.get(overdueIds.size() - 1);
                if (overdueIds.size() < BATCH_SIZE) {
                    break;
                }
            }
            if (processed > 0) {
                log.info("定时任务超时关单完成: 处理 {} 单", processed);
            }
        } catch (Exception e) {
            log.error("定时任务超时关单异常，下轮继续", e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
