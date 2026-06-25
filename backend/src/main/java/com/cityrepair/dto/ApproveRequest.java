package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequest(
        @NotBlank String priority,
        String remark
) {}
