package com.antique.config;

import com.antique.constant.RedisConstant;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 订单超时延迟队列配置 — Redisson RDelayedQueue
 *
 * <p>Key: {@code delay:order-close}，元素 = orderId，创建订单时 offer（延迟 16 分钟），
 * 到期后自动投递到阻塞队列，由 {@code OrderTimeoutConsumer} 阻塞 take 消费。
 *
 * <p>可靠性说明：延迟消息允许丢失（Redis 重启/消费失败），
 * 超时关单由定时任务与惰性检查兜底，正确性不依赖本队列（见技术方案文档）。
 */
@Configuration
public class OrderTimeoutConfig {

    /**
     * 底层阻塞队列 Bean（消费端 take 用）
     *
     * <p>与 {@link #orderDelayQueue} 指向同一 Redis 队列，
     * 到期元素由 Redisson 自动从延迟队列转移到本阻塞队列。
     */
    @Bean
    public RBlockingQueue<Long> orderBlockingQueue(RedissonClient redissonClient) {
        return redissonClient.getBlockingQueue(RedisConstant.KEY_ORDER_DELAY_QUEUE);
    }

    /**
     * 订单超时延迟队列 Bean（生产端 offer 用）
     *
     * <p>destroyMethod 指向 RDelayedQueue#destroy()：应用关闭时清理本地延迟调度，
     * Redis 中未到期的元素仍在队列中，下次启动消费可继续处理。
     */
    @Bean(destroyMethod = "destroy")
    public RDelayedQueue<Long> orderDelayQueue(RedissonClient redissonClient,
                                               RBlockingQueue<Long> orderBlockingQueue) {
        return redissonClient.getDelayedQueue(orderBlockingQueue);
    }
}
