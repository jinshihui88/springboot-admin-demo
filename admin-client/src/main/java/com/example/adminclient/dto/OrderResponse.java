package com.example.adminclient.dto;

/**
 * 订单响应。
 */
public record OrderResponse(long id, String product, int quantity, long createdAt) {
}
