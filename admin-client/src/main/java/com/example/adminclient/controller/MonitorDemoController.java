package com.example.adminclient.controller;

import com.example.adminclient.common.ApiResult;
import com.example.adminclient.health.PaymentChannelHealthIndicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控演示接口：切换依赖健康状态、按级别输出日志，
 * 用于在 Admin 界面上观察健康变化与日志级别动态调整效果。
 */
@RestController
@RequestMapping("/api/demo")
public class MonitorDemoController {

    private static final Logger log = LoggerFactory.getLogger("com.example.adminclient.business");

    private final PaymentChannelHealthIndicator paymentChannelHealth;

    public MonitorDemoController(PaymentChannelHealthIndicator paymentChannelHealth) {
        this.paymentChannelHealth = paymentChannelHealth;
    }

    /** 切换模拟依赖的健康状态：healthy=false 时应用整体健康变为 DOWN */
    @PostMapping("/dependency")
    public ApiResult<Map<String, Object>> toggleDependency(@RequestParam boolean healthy) {
        paymentChannelHealth.setUp(healthy);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("paymentChannelUp", paymentChannelHealth.isUp());
        return ApiResult.ok(state);
    }

    /** 按指定级别输出一条演示日志，验证动态调级后新级别立即生效 */
    @GetMapping("/log")
    public ApiResult<String> emitLog(@RequestParam(defaultValue = "info") String level) {
        String message = "这是一条用于验证日志级别的演示日志，当前级别: " + level;
        switch (level.toLowerCase()) {
            case "debug" -> log.debug(message);
            case "warn" -> log.warn(message);
            case "error" -> log.error(message);
            default -> log.info(message);
        }
        return ApiResult.ok("已输出 " + level.toUpperCase() + " 日志");
    }
}
