package com.example.adminclient.repository;

import com.example.adminclient.dto.OrderResponse;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存订单存储：重启即重置，教程演示用。
 */
@Repository
public class OrderRepository {

    private final AtomicLong idGen = new AtomicLong();
    private final Map<Long, OrderResponse> store = new ConcurrentHashMap<>();
    /** 记录插入顺序，用于列表稳定排序 */
    private final List<Long> order = new CopyOnWriteArrayList<>();

    public OrderResponse save(String product, int quantity) {
        long id = idGen.incrementAndGet();
        OrderResponse resp = new OrderResponse(id, product, quantity, System.currentTimeMillis());
        store.put(id, resp);
        order.add(id);
        return resp;
    }

    public List<OrderResponse> findAll() {
        List<Long> ids = new ArrayList<>(order);
        ids.sort(Comparator.reverseOrder());
        return ids.stream().map(store::get).filter(java.util.Objects::nonNull).toList();
    }

    public Optional<OrderResponse> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }
}
