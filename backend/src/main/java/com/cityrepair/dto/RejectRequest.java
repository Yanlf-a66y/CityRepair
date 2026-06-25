package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因最长500字")
        String reason
) {}
