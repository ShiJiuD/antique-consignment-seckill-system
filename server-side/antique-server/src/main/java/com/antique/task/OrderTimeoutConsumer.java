package com.antique.task;

import com.antique.service.OrderService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 订单超时延迟消息消费者 — 阻塞消费到期订单并执行超时关单
 *
 * <p>启动后常驻一个线程循环 {@code take()}（阻塞等待），
 * 到期消息取出后调用 {@code closeOrderByTimeout}（条件更新幂等，
 * 与定时任务/惰性检查并发安全）。
 *
 * <p>消费异常仅记日志不中断循环：该消息不再重试，交由定时任务兜底
 * （延迟消息允许丢失，正确性不依赖本消费者）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutConsumer implements CommandLineRunner {

    private final RBlockingQueue<Long> orderBlockingQueue;
    private final OrderService orderService;

    /** 消费线程（常驻，应用关闭时随 shutdownNow 中断退出） */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "order-timeout-consumer");
        t.setDaemon(true);
        return t;
    });

    /** 运行标记，@PreDestroy 置 false 退出循环 */
    private volatile boolean running = true;

    @Override
    public void run(String... args) {
        executor.submit(this::consumeLoop);
        log.info("订单超时延迟消息消费者已启动");
    }

    /**
     * 消费循环：阻塞取到期订单 → 超时关单
     */
    private void consumeLoop() {
        while (running) {
            try {
                Long orderId = orderBlockingQueue.take();
                log.info("延迟消息到期，执行超时关单: orderId={}", orderId);
                orderService.closeOrderByTimeout(orderId);
            } catch (InterruptedException e) {
                // 应用关闭信号，退出循环
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 消费失败不阻塞循环，该订单交由定时任务兜底
                log.error("延迟消息消费异常，交由定时任务兜底: error={}", e.getMessage(), e);
            }
        }
        log.info("订单超时延迟消息消费者已停止");
    }

    /**
     * 应用关闭时停止消费线程
     */
    @PreDestroy
    public void shutdown() {
        running = false;
        executor.shutdownNow();
    }
}
