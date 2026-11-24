package com.example.adminclient.common;

/**
 * 统一响应结构。
 */
public record ApiResult<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data, System.currentTimeMillis());
    }

    public static <T> ApiResult<T> error(int code, String message) {
        return new ApiResult<>(code, message, null, System.currentTimeMillis());
    }
}
