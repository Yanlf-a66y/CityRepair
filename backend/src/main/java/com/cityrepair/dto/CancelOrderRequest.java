package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelOrderRequest {
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因不超过500字")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
