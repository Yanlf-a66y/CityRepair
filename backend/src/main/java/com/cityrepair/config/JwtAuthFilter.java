package com.cityrepair.config;

import com.cityrepair.common.AuthContext;
import com.cityrepair.common.AuthContext.CurrentUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login", "/api/auth/demo-accounts");
    private static final Set<String> PUBLIC_PREFIXES = Set.of("/api/health");

    private final JwtUtil jwtUtil;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public JwtAuthFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String token = extractToken(request);
            if (token == null) {
                sendError(response, 401, "请先登录");
                return;
            }
            Claims claims = jwtUtil.parse(token);
            AuthContext.set(new CurrentUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class)
            ));
        } catch (ExpiredJwtException e) {
            sendError(response, 401, "登录已过期");
            return;
        } catch (RuntimeException e) {
            sendError(response, 401, "无效token");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private boolean isPublic(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        for (String prefix : PUBLIC_PREFIXES) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, int code, String msg)
            throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        var body = com.cityrepair.common.ApiResponse.error(code, msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(
            JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
