package com.cityrepair.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName,
        String role
) {}
