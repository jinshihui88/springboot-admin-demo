package com.example.adminclient.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 自定义健康指示器：模拟一个外部依赖（如第三方支付通道）。
 *
 * 通过 {@link #setUp(boolean)} 切换状态，用于演示
 * 依赖异常时健康状态从 UP 变 DOWN、恢复后变回 UP 的完整链路。
 */
@Component("payment-channel")
public class PaymentChannelHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(PaymentChannelHealthIndicator.class);

    private final AtomicBoolean up = new AtomicBoolean(true);

    @Override
    public Health health() {
        if (up.get()) {
            return Health.up().withDetail("channel", "main").build();
        }
        return Health.down().withDetail("channel", "main").withDetail("error", "payment channel unreachable").build();
    }

    public void setUp(boolean healthy) {
        up.set(healthy);
        log.warn("支付通道健康状态切换为 {}", healthy ? "UP" : "DOWN");
    }

    public boolean isUp() {
        return up.get();
    }
}
