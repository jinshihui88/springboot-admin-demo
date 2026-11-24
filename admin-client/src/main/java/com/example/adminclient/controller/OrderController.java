package com.example.adminclient.controller;

import com.example.adminclient.common.ApiResult;
import com.example.adminclient.dto.OrderCreateRequest;
import com.example.adminclient.dto.OrderResponse;
import com.example.adminclient.exception.ResourceNotFoundException;
import com.example.adminclient.repository.OrderRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单接口：为监控提供真实业务流量与日志输出。
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository repository;

    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ApiResult<OrderResponse> create(@Valid @RequestBody OrderCreateRequest request) {
        OrderResponse created = repository.save(request.product(), request.quantity());
        log.info("订单创建成功 id={} product={} quantity={}",
                created.id(), created.product(), created.quantity());
        return ApiResult.ok(created);
    }

    @GetMapping
    public ApiResult<List<OrderResponse>> list() {
        log.debug("查询订单列表，当前共 {} 条", repository.findAll().size());
        return ApiResult.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResult<OrderResponse> detail(@PathVariable long id) {
        OrderResponse order = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + id));
        log.debug("查询订单详情 id={}", id);
        return ApiResult.ok(order);
    }
}
