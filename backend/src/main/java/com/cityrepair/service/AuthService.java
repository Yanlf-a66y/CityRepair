package com.cityrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cityrepair.common.ApiResponse;
import com.cityrepair.config.JwtUtil;
import com.cityrepair.dto.LoginRequest;
import com.cityrepair.dto.LoginResponse;
import com.cityrepair.entity.SysRole;
import com.cityrepair.entity.SysUser;
import com.cityrepair.entity.SysUserRole;
import com.cityrepair.mapper.SysRoleMapper;
import com.cityrepair.mapper.SysUserMapper;
import com.cityrepair.mapper.SysUserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jwtUtil = jwtUtil;
    }

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username()));
        if (user == null) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            return ApiResponse.error(403, "账号已被禁用");
        }
        // {noop} plaintext password check
        String expectedPassword = "{noop}" + request.password();
        if (!expectedPassword.equals(user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }
        String role = getUserRole(user.getId());
        String token = jwtUtil.generate(user.getId(), user.getUsername(), role);
        return ApiResponse.success(new LoginResponse(
                token, user.getId(), user.getUsername(), user.getRealName(), role));
    }

    private String getUserRole(Long userId) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return "RESIDENT";
        SysRole role = roleMapper.selectById(userRoles.get(0).getRoleId());
        return role != null ? role.getRoleCode() : "RESIDENT";
    }
}
