package com.cityrepair.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull(message = "维修人员ID不能为空")
        Long workerId,
        String remark
) {}
