package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.AuthContext;
import com.cityrepair.common.AuthContext.CurrentUser;
import com.cityrepair.dto.LoginRequest;
import com.cityrepair.dto.LoginResponse;
import com.cityrepair.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/demo-accounts")
    public ApiResponse<List<Map<String, String>>> demoAccounts() {
        return ApiResponse.success(List.of(
                Map.of("role", "ADMIN", "username", "admin", "password", "123456"),
                Map.of("role", "RESIDENT", "username", "resident1", "password", "123456"),
                Map.of("role", "WORKER", "username", "worker1", "password", "123456")
        ));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        CurrentUser user = AuthContext.get();
        if (user == null) {
            return ApiResponse.error(401, "未登录");
        }
        return ApiResponse.success(Map.of(
                "userId", user.userId(),
                "username", user.username(),
                "role", user.role()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("已退出登录", null);
    }
}
