package com.example.adminclient.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 订单创建请求。
 */
public record OrderCreateRequest(

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 64, message = "商品名称长度不能超过 64")
        String product,

        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 9999, message = "数量不能超过 9999")
        int quantity
) {
}
