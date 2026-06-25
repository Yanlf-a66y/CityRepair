# 管理员审核与分派模块 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the full admin dispatch module — JWT auth, order query, approve/reject/assign, status logs, and the frontend admin orders page.

**Architecture:** Spring Boot 3 backend with MyBatis-Plus, JWT-based auth filter, ThreadLocal user context, @Transactional service layer. Vue 3 + Element Plus frontend with single-page admin orders view containing table, filters, dialogs and log drawer.

**Tech Stack:** Spring Boot 3.3.6, MyBatis-Plus 3.5.9, Druid, JWT (io.jsonwebtoken), Vue 3.5, Element Plus 2.9, Axios, TypeScript

**Spec:** `docs/superpowers/specs/2026-06-25-admin-dispatch-design.md`

---

## File Structure

```
backend/src/main/java/com/cityrepair/
├── common/
│   ├── ApiResponse.java          (existing — no changes)
│   ├── AuthContext.java           NEW: ThreadLocal user holder
│   └── PageQuery.java            (existing — no changes)
├── config/
│   ├── CorsConfig.java           (existing — no changes)
│   ├── JwtUtil.java              NEW: JWT generate/parse
│   └── JwtAuthFilter.java        NEW: OncePerRequestFilter
├── controller/
│   ├── AuthController.java       MODIFY: add login/me/logout
│   ├── AdminOrderController.java MODIFY: full implementation
│   ├── RepairOrderController.java MODIFY: add GET /{id}/logs
│   ├── HealthController.java     (existing — no changes)
│   └── StatisticsController.java (existing — no changes)
├── dto/
│   ├── LoginRequest.java         NEW
│   ├── LoginResponse.java        NEW
│   ├── ApproveRequest.java       NEW
│   ├── RejectRequest.java        NEW
│   └── AssignRequest.java        NEW
├── entity/
│   ├── SysUser.java              NEW
│   ├── SysRole.java              NEW
│   ├── SysUserRole.java          NEW
│   ├── RepairOrder.java          NEW
│   ├── OrderAssignment.java      NEW
│   └── OrderStatusLog.java       NEW
├── enums/
│   ├── OrderPriority.java        (existing — no changes)
│   ├── OrderStatus.java          (existing — no changes)
│   └── UserRole.java             (existing — no changes)
├── mapper/
│   ├── SysUserMapper.java        NEW
│   ├── SysRoleMapper.java        NEW
│   ├── SysUserRoleMapper.java    NEW
│   ├── RepairOrderMapper.java    NEW
│   ├── OrderAssignmentMapper.java NEW
│   └── OrderStatusLogMapper.java NEW
└── service/
    ├── AuthService.java          NEW
    └── AdminOrderService.java    NEW

frontend/src/
├── api/
│   ├── http.ts                   (existing — no changes)
│   ├── systemApi.ts              (existing — no changes)
│   ├── adminApi.ts               NEW
│   └── repairOrderApi.ts         NEW
├── views/
│   └── AdminOrdersView.vue       MODIFY: full rewrite
├── types/
│   └── order.ts                  (existing — no changes)
```

---

### Task 1: JWT 认证基础设施

**Files:**
- Create: `backend/src/main/java/com/cityrepair/common/AuthContext.java`
- Create: `backend/src/main/java/com/cityrepair/config/JwtUtil.java`
- Create: `backend/src/main/java/com/cityrepair/config/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/cityrepair/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/cityrepair/dto/LoginResponse.java`

- [ ] **Step 1: Create AuthContext.java**

```java
package com.cityrepair.common;

public final class AuthContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(CurrentUser user) { HOLDER.set(user); }
    public static CurrentUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public record CurrentUser(Long userId, String username, String role) {}
}
```

- [ ] **Step 2: Create LoginRequest.java**

```java
package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {}
```

- [ ] **Step 3: Create LoginResponse.java**

```java
package com.cityrepair.dto;

public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName,
        String role
) {}
```

- [ ] **Step 4: Create ApproveRequest.java**

```java
package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveRequest(
        @NotBlank String priority,
        String remark
) {}
```

- [ ] **Step 5: Create RejectRequest.java**

```java
package com.cityrepair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 500, message = "驳回原因最长500字")
        String reason
) {}
```

- [ ] **Step 6: Create AssignRequest.java**

```java
package com.cityrepair.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(
        @NotNull(message = "维修人员ID不能为空")
        Long workerId,
        String remark
) {}
```

- [ ] **Step 7: Create JwtUtil.java**

```java
package com.cityrepair.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generate(Long userId, String username, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

Note: `pom.xml` already has `spring-boot-starter-web` which pulls in Jackson. We need `jjwt-api`, `jjwt-impl`, `jjwt-jackson` — add to pom.xml:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
```

- [ ] **Step 8: Create JwtAuthFilter.java**

```java
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

    private static final Set<String> PUBLIC_PATHS = Set.of("/api/auth/login", "/api/auth/demo-accounts");
    private static final Set<String> PUBLIC_PREFIXES = Set.of("/api/health");

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) { this.jwtUtil = jwtUtil; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String token = extractToken(request);
            if (token == null) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null,\"timestamp\":null}");
                return;
            }
            Claims claims = jwtUtil.parse(token);
            AuthContext.set(new CurrentUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class)
            ));
            chain.doFilter(request, response);
        } catch (ExpiredJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期\",\"data\":null,\"timestamp\":null}");
        } catch (SignatureException | MalformedJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"无效token\",\"data\":null,\"timestamp\":null}");
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

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return reg;
    }
}
```

- [ ] **Step 9: Add jjwt dependencies to pom.xml**

Modify `backend/pom.xml`, add inside `<dependencies>` after the mysql dependency:

```xml
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
```

- [ ] **Step 10: Compile check**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS (if no typos)

- [ ] **Step 11: Commit**

```bash
git add backend/pom.xml backend/src/main/java/com/cityrepair/common/AuthContext.java backend/src/main/java/com/cityrepair/config/JwtUtil.java backend/src/main/java/com/cityrepair/config/JwtAuthFilter.java backend/src/main/java/com/cityrepair/dto/
git commit -m "feat: add JWT auth infrastructure (JwtUtil, JwtAuthFilter, AuthContext, DTOs)"
```

---

### Task 2: 实体类和 Mapper

**Files:**
- Create: `backend/src/main/java/com/cityrepair/entity/SysUser.java`
- Create: `backend/src/main/java/com/cityrepair/entity/SysRole.java`
- Create: `backend/src/main/java/com/cityrepair/entity/SysUserRole.java`
- Create: `backend/src/main/java/com/cityrepair/entity/RepairOrder.java`
- Create: `backend/src/main/java/com/cityrepair/entity/OrderAssignment.java`
- Create: `backend/src/main/java/com/cityrepair/entity/OrderStatusLog.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/SysUserMapper.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/SysRoleMapper.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/SysUserRoleMapper.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/RepairOrderMapper.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/OrderAssignmentMapper.java`
- Create: `backend/src/main/java/com/cityrepair/mapper/OrderStatusLogMapper.java`

- [ ] **Step 1: Create SysUser.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String email;
    private Integer enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create SysRole.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Create SysUserRole.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_user_role")
public class SysUserRole {
    private Long userId;
    private Long roleId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
}
```

- [ ] **Step 4: Create RepairOrder.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("repair_order")
public class RepairOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long residentId;
    private Long categoryId;
    private String title;
    private String location;
    private String description;
    private String contactPhone;
    private String priority;
    private String status;
    private Long currentWorkerId;
    private String rejectReason;
    private String cancelReason;
    private String completionResult;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;
    private LocalDateTime evaluatedAt;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters / setters (all)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getResidentId() { return residentId; }
    public void setResidentId(Long residentId) { this.residentId = residentId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCurrentWorkerId() { return currentWorkerId; }
    public void setCurrentWorkerId(Long currentWorkerId) { this.currentWorkerId = currentWorkerId; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getCompletionResult() { return completionResult; }
    public void setCompletionResult(String completionResult) { this.completionResult = completionResult; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 5: Create OrderAssignment.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_assignment")
public class OrderAssignment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long adminId;
    private Long workerId;
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime canceledAt;
    private Integer isCurrent;
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getAdminId() { return adminId; }
    public void setAdminId(Long adminId) { this.adminId = adminId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public Integer getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Integer isCurrent) { this.isCurrent = isCurrent; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
```

- [ ] **Step 6: Create OrderStatusLog.java**

```java
package com.cityrepair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("order_status_log")
public class OrderStatusLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long operatorId;
    private String action;
    private String fromStatus;
    private String toStatus;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 7: Create SysUserMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

- [ ] **Step 8: Create SysRoleMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
```

- [ ] **Step 9: Create SysUserRoleMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
```

- [ ] **Step 10: Create RepairOrderMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {
}
```

- [ ] **Step 11: Create OrderAssignmentMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.OrderAssignment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderAssignmentMapper extends BaseMapper<OrderAssignment> {
}
```

- [ ] **Step 12: Create OrderStatusLogMapper.java**

```java
package com.cityrepair.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cityrepair.entity.OrderStatusLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderStatusLogMapper extends BaseMapper<OrderStatusLog> {
}
```

- [ ] **Step 13: Compile check**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 14: Commit**

```bash
git add backend/src/main/java/com/cityrepair/entity/ backend/src/main/java/com/cityrepair/mapper/
git commit -m "feat: add entity classes and MyBatis-Plus mappers for all business tables"
```

---

### Task 3: AuthService 和 AuthController

**Files:**
- Create: `backend/src/main/java/com/cityrepair/service/AuthService.java`
- Modify: `backend/src/main/java/com/cityrepair/controller/AuthController.java`

- [ ] **Step 1: Create AuthService.java**

```java
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

    public ApiResponse<List<String>> demoAccounts() {
        return ApiResponse.success(java.util.List.of(
                java.util.Map.of("role", "ADMIN", "username", "admin", "password", "123456"),
                java.util.Map.of("role", "RESIDENT", "username", "resident1", "password", "123456"),
                java.util.Map.of("role", "WORKER", "username", "worker1", "password", "123456")
        ).stream().map(Object::toString).toList());
    }
}
```

- [ ] **Step 2: Modify AuthController.java**

Replace the entire file content:

```java
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
```

- [ ] **Step 3: Compile check**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/cityrepair/service/AuthService.java backend/src/main/java/com/cityrepair/controller/AuthController.java
git commit -m "feat: implement login/me/logout with JWT token and {noop} password auth"
```

---

### Task 4: AdminOrderService 和 AdminOrderController

**Files:**
- Create: `backend/src/main/java/com/cityrepair/service/AdminOrderService.java`
- Modify: `backend/src/main/java/com/cityrepair/controller/AdminOrderController.java`
- Modify: `backend/src/main/java/com/cityrepair/controller/RepairOrderController.java`

- [ ] **Step 1: Create AdminOrderService.java**

```java
package com.cityrepair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.AuthContext;
import com.cityrepair.common.PageQuery;
import com.cityrepair.dto.ApproveRequest;
import com.cityrepair.dto.AssignRequest;
import com.cityrepair.dto.RejectRequest;
import com.cityrepair.entity.*;
import com.cityrepair.enums.OrderStatus;
import com.cityrepair.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminOrderService {

    private final RepairOrderMapper orderMapper;
    private final OrderAssignmentMapper assignmentMapper;
    private final OrderStatusLogMapper statusLogMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public AdminOrderService(RepairOrderMapper orderMapper, OrderAssignmentMapper assignmentMapper,
                             OrderStatusLogMapper statusLogMapper, SysUserMapper userMapper,
                             SysUserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
        this.orderMapper = orderMapper;
        this.assignmentMapper = assignmentMapper;
        this.statusLogMapper = statusLogMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    // ----- role guard -----
    private void requireAdmin() {
        var user = AuthContext.get();
        if (user == null || !"ADMIN".equals(user.role())) {
            throw new RuntimeException("403:无权访问，需要管理员权限");
        }
    }

    private Long currentUserId() {
        return AuthContext.get().userId();
    }

    // ----- query -----
    public ApiResponse<Map<String, Object>> listOrders(PageQuery query) {
        requireAdmin();
        LambdaQueryWrapper<RepairOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(RepairOrder::getTitle, query.getKeyword())
                    .or().like(RepairOrder::getOrderNo, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RepairOrder::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(RepairOrder::getCreatedAt);
        Page<RepairOrder> page = orderMapper.selectPage(
                new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return ApiResponse.success(Map.of(
                "page", page.getCurrent(),
                "pageSize", page.getSize(),
                "total", page.getTotal(),
                "records", page.getRecords()
        ));
    }

    // ----- approve -----
    @Transactional
    public ApiResponse<Void> approve(Long orderId, ApproveRequest request) {
        requireAdmin();
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待审核工单可以审核");
        }
        if (!OrderStatus.PENDING_REVIEW.canMoveTo(OrderStatus.PENDING_ASSIGN)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.PENDING_ASSIGN.name());
        order.setPriority(request.priority());
        orderMapper.updateById(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(currentUserId());
        log.setAction("APPROVE");
        log.setFromStatus(OrderStatus.PENDING_REVIEW.name());
        log.setToStatus(OrderStatus.PENDING_ASSIGN.name());
        log.setRemark(request.remark());
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
        return ApiResponse.success("审核通过", null);
    }

    // ----- reject -----
    @Transactional
    public ApiResponse<Void> reject(Long orderId, RejectRequest request) {
        requireAdmin();
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_REVIEW.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待审核工单可以驳回");
        }
        if (!OrderStatus.PENDING_REVIEW.canMoveTo(OrderStatus.REJECTED)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.REJECTED.name());
        order.setRejectReason(request.reason());
        orderMapper.updateById(order);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(currentUserId());
        log.setAction("REJECT");
        log.setFromStatus(OrderStatus.PENDING_REVIEW.name());
        log.setToStatus(OrderStatus.REJECTED.name());
        log.setRemark(request.reason());
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
        return ApiResponse.success("已驳回", null);
    }

    // ----- assign -----
    @Transactional
    public ApiResponse<Void> assign(Long orderId, AssignRequest request) {
        requireAdmin();
        RepairOrder order = orderMapper.selectById(orderId);
        if (order == null) return ApiResponse.error(404, "工单不存在");
        if (!OrderStatus.PENDING_ASSIGN.name().equals(order.getStatus())) {
            return ApiResponse.error(400, "只有待分派工单可以分派");
        }
        // verify worker exists and has WORKER role
        SysUser worker = userMapper.selectById(request.workerId());
        if (worker == null || worker.getEnabled() == null || worker.getEnabled() != 1) {
            return ApiResponse.error(400, "维修人员不存在或已禁用");
        }
        boolean isWorker = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, request.workerId()))
                .stream().anyMatch(ur -> {
                    SysRole role = roleMapper.selectById(ur.getRoleId());
                    return role != null && "WORKER".equals(role.getRoleCode());
                });
        if (!isWorker) {
            return ApiResponse.error(400, "被分派人必须具有维修人员(WORKER)角色");
        }
        if (!OrderStatus.PENDING_ASSIGN.canMoveTo(OrderStatus.PENDING_ACCEPT)) {
            return ApiResponse.error(400, "非法状态流转");
        }
        order.setStatus(OrderStatus.PENDING_ACCEPT.name());
        order.setCurrentWorkerId(request.workerId());
        order.setAssignedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        OrderAssignment assignment = new OrderAssignment();
        assignment.setOrderId(orderId);
        assignment.setAdminId(currentUserId());
        assignment.setWorkerId(request.workerId());
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setIsCurrent(1);
        assignment.setRemark(request.remark());
        assignmentMapper.insert(assignment);

        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOperatorId(currentUserId());
        log.setAction("ASSIGN");
        log.setFromStatus(OrderStatus.PENDING_ASSIGN.name());
        log.setToStatus(OrderStatus.PENDING_ACCEPT.name());
        log.setRemark("分派给维修人员 #" + request.workerId()
                + (StringUtils.hasText(request.remark()) ? "：" + request.remark() : ""));
        log.setCreatedAt(LocalDateTime.now());
        statusLogMapper.insert(log);
        return ApiResponse.success("分派成功", null);
    }

    // ----- status logs -----
    public ApiResponse<List<OrderStatusLog>> getLogs(Long orderId) {
        List<OrderStatusLog> logs = statusLogMapper.selectList(
                new LambdaQueryWrapper<OrderStatusLog>()
                        .eq(OrderStatusLog::getOrderId, orderId)
                        .orderByAsc(OrderStatusLog::getCreatedAt));
        return ApiResponse.success(logs);
    }

    // ----- worker list for assign dropdown -----
    public ApiResponse<List<SysUser>> getWorkers() {
        requireAdmin();
        // find all users with WORKER role
        List<SysUserRole> workerRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, 3L));
        List<Long> workerIds = workerRoles.stream().map(SysUserRole::getUserId).toList();
        if (workerIds.isEmpty()) return ApiResponse.success(List.of());
        List<SysUser> workers = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, workerIds)
                        .eq(SysUser::getEnabled, 1));
        return ApiResponse.success(workers);
    }
}
```

- [ ] **Step 2: Modify AdminOrderController.java**

Replace stub with full implementation:

```java
package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.PageQuery;
import com.cityrepair.dto.ApproveRequest;
import com.cityrepair.dto.AssignRequest;
import com.cityrepair.dto.RejectRequest;
import com.cityrepair.entity.OrderStatusLog;
import com.cityrepair.entity.SysUser;
import com.cityrepair.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> orders(@ModelAttribute PageQuery query) {
        return adminOrderService.listOrders(query);
    }

    @PutMapping("/orders/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id, @Valid @RequestBody ApproveRequest request) {
        return adminOrderService.approve(id, request);
    }

    @PutMapping("/orders/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request) {
        return adminOrderService.reject(id, request);
    }

    @PutMapping("/orders/{id}/assign")
    public ApiResponse<Void> assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return adminOrderService.assign(id, request);
    }

    @GetMapping("/workers")
    public ApiResponse<List<SysUser>> workers() {
        return adminOrderService.getWorkers();
    }
}
```

- [ ] **Step 3: Modify RepairOrderController.java — add GET /{id}/logs**

Replace the file, keeping existing stubs and adding the logs endpoint:

```java
package com.cityrepair.controller;

import com.cityrepair.common.ApiResponse;
import com.cityrepair.common.PageQuery;
import com.cityrepair.entity.OrderStatusLog;
import com.cityrepair.enums.OrderStatus;
import com.cityrepair.service.AdminOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/repair-orders")
public class RepairOrderController {

    private final AdminOrderService adminOrderService;

    public RepairOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping("/statuses")
    public ApiResponse<List<String>> statuses() {
        return ApiResponse.success(List.of(OrderStatus.values()).stream()
                .map(Enum::name).toList());
    }

    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myOrders(@ModelAttribute PageQuery query) {
        return ApiResponse.success(Map.of(
                "page", query.getPage(),
                "pageSize", query.getPageSize(),
                "total", 0,
                "records", List.of()
        ));
    }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<OrderStatusLog>> logs(@PathVariable Long id) {
        return adminOrderService.getLogs(id);
    }
}
```

- [ ] **Step 4: Add global @ControllerAdvice for exception handling**

Create: `backend/src/main/java/com/cityrepair/config/GlobalExceptionHandler.java`

```java
package com.cityrepair.config;

import com.cityrepair.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (msg != null && msg.startsWith("403:")) {
            return ApiResponse.error(403, msg.substring(4));
        }
        return ApiResponse.error(500, msg != null ? msg : "服务器内部错误");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b).orElse("参数校验失败");
        return ApiResponse.error(400, msg);
    }
}
```

- [ ] **Step 5: Compile check**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Start backend and test login**

```bash
# In one terminal:
cd backend && mvn spring-boot:run

# In another terminal, test login:
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

Expected: `{"code":0,"message":"success","data":{"token":"eyJ...","userId":1,"username":"admin","realName":"系统管理员","role":"ADMIN"},"timestamp":"..."}`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/cityrepair/service/AdminOrderService.java backend/src/main/java/com/cityrepair/controller/AdminOrderController.java backend/src/main/java/com/cityrepair/controller/RepairOrderController.java backend/src/main/java/com/cityrepair/config/GlobalExceptionHandler.java
git commit -m "feat: implement admin order query, approve, reject, assign with full transaction and validation"
```

---

### Task 5: 前端 API 封装

**Files:**
- Create: `frontend/src/api/adminApi.ts`
- Create: `frontend/src/api/repairOrderApi.ts`

- [ ] **Step 1: Create adminApi.ts**

```typescript
import { http, type ApiResponse } from './http'

export interface RepairOrder {
  id: number
  orderNo: string
  residentId: number
  categoryId: number
  title: string
  location: string
  description: string
  contactPhone: string
  priority: string
  status: string
  currentWorkerId: number | null
  rejectReason: string | null
  completionResult: string | null
  createdAt: string
  updatedAt: string
}

export interface OrderStatusLog {
  id: number
  orderId: number
  operatorId: number
  action: string
  fromStatus: string | null
  toStatus: string
  remark: string | null
  createdAt: string
}

export interface OrderListResult {
  page: number
  pageSize: number
  total: number
  records: RepairOrder[]
}

export interface WorkerInfo {
  id: number
  username: string
  realName: string
  phone: string
}

export interface OrderListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: string
}

export function getAdminOrders(params: OrderListParams) {
  return http.get<ApiResponse<OrderListResult>>('/admin/orders', { params })
}

export function approveOrder(id: number, body: { priority: string; remark?: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/approve`, body)
}

export function rejectOrder(id: number, body: { reason: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/reject`, body)
}

export function assignOrder(id: number, body: { workerId: number; remark?: string }) {
  return http.put<ApiResponse<null>>(`/admin/orders/${id}/assign`, body)
}

export function getWorkerList() {
  return http.get<ApiResponse<WorkerInfo[]>>('/admin/workers')
}
```

- [ ] **Step 2: Create repairOrderApi.ts**

```typescript
import { http, type ApiResponse } from './http'
import type { OrderStatusLog } from './adminApi'

export function getOrderStatuses() {
  return http.get<ApiResponse<string[]>>('/repair-orders/statuses')
}

export function getOrderLogs(id: number) {
  return http.get<ApiResponse<OrderStatusLog[]>>(`/repair-orders/${id}/logs`)
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/adminApi.ts frontend/src/api/repairOrderApi.ts
git commit -m "feat: add admin and repair-order API wrappers for frontend"
```

---

### Task 6: 前端 AdminOrdersView.vue 完整页面

**Files:**
- Modify: `frontend/src/views/AdminOrdersView.vue`

- [ ] **Step 1: Rewrite AdminOrdersView.vue**

```vue
<template>
  <section class="page">
    <div class="page-header">
      <div>
        <h1 class="page-title">审核与分派</h1>
        <p class="muted">管理员审核、驳回、设置优先级和分派维修人员</p>
      </div>
      <el-button :loading="loading" @click="fetchOrders">刷新</el-button>
    </div>

    <!-- Filters -->
    <div class="panel filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索工单编号或标题"
        clearable
        style="width: 220px"
        @clear="search"
        @keyup.enter="search"
      />
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px" @change="search">
        <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
      </el-select>
      <el-button type="primary" @click="search" :loading="loading">查询</el-button>
    </div>

    <!-- Table -->
    <div class="panel table-panel">
      <el-table v-loading="loading" :data="orders" stripe empty-text="暂无工单" style="width: 100%">
        <el-table-column prop="orderNo" label="工单编号" min-width="150" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityTag(row.priority)" size="small" effect="light">
              {{ priorityLabel(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="light">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提交时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING_REVIEW'">
              <el-button type="primary" size="small" @click="openApprove(row)">审核通过</el-button>
              <el-button type="danger" size="small" @click="openReject(row)">驳回</el-button>
            </template>
            <template v-else-if="row.status === 'PENDING_ASSIGN'">
              <el-button type="primary" size="small" @click="openAssign(row)">分派</el-button>
            </template>
            <template v-else>
              <el-button size="small" @click="openLogs(row)">日志</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="filters.page"
          v-model:page-size="filters.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="fetchOrders"
        />
      </div>
    </div>

    <!-- Approve Dialog -->
    <el-dialog v-model="approveVisible" title="审核通过" width="480px">
      <el-form label-width="80px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="标题">{{ currentOrder?.title }}</el-form-item>
        <el-form-item label="优先级" required>
          <el-select v-model="approveForm.priority" style="width: 100%">
            <el-option v-for="p in priorities" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="approveForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="doApprove">确认通过</el-button>
      </template>
    </el-dialog>

    <!-- Reject Dialog -->
    <el-dialog v-model="rejectVisible" title="驳回工单" width="480px">
      <el-form label-width="80px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            maxlength="500"
            show-word-limit
            placeholder="请填写驳回原因（必填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitting" :disabled="!rejectForm.reason.trim()" @click="doReject">
          确认驳回
        </el-button>
      </template>
    </el-dialog>

    <!-- Assign Dialog -->
    <el-dialog v-model="assignVisible" title="分派维修人员" width="480px">
      <el-form label-width="100px">
        <el-form-item label="工单编号"><strong>{{ currentOrder?.orderNo }}</strong></el-form-item>
        <el-form-item label="维修人员" required>
          <el-select v-model="assignForm.workerId" style="width: 100%" placeholder="请选择维修人员">
            <el-option
              v-for="w in workers"
              :key="w.id"
              :label="`${w.realName} (${w.username})`"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.remark" type="textarea" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!assignForm.workerId" @click="doAssign">
          确认分派
        </el-button>
      </template>
    </el-dialog>

    <!-- Status Log Drawer -->
    <el-drawer v-model="logVisible" title="状态日志" size="450px">
      <el-timeline v-if="logs.length">
        <el-timeline-item
          v-for="log in logs"
          :key="log.id"
          :timestamp="formatTime(log.createdAt)"
          placement="top"
        >
          <p><strong>{{ log.action }}</strong>: {{ log.fromStatus || '—' }} → {{ log.toStatus }}</p>
          <p v-if="log.remark" class="muted" style="font-size:13px">{{ log.remark }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无日志" />
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminOrders,
  approveOrder,
  rejectOrder,
  assignOrder,
  getWorkerList,
  type RepairOrder,
  type OrderStatusLog,
  type WorkerInfo,
} from '@/api/adminApi'
import { getOrderLogs } from '@/api/repairOrderApi'

// ---- state ----
const loading = ref(false)
const submitting = ref(false)
const orders = ref<RepairOrder[]>([])
const total = ref(0)
const workers = ref<WorkerInfo[]>([])

const filters = reactive({ page: 1, pageSize: 10, keyword: '', status: '' as string })

const statuses = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '待分派', value: 'PENDING_ASSIGN' },
  { label: '待接单', value: 'PENDING_ACCEPT' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已评价', value: 'EVALUATED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const priorities = [
  { label: '低', value: 'LOW' },
  { label: '普通', value: 'NORMAL' },
  { label: '高', value: 'HIGH' },
  { label: '紧急', value: 'URGENT' },
]

// dialog state
const approveVisible = ref(false)
const rejectVisible = ref(false)
const assignVisible = ref(false)
const logVisible = ref(false)
const currentOrder = ref<RepairOrder | null>(null)
const logs = ref<OrderStatusLog[]>([])

const approveForm = reactive({ priority: 'NORMAL', remark: '' })
const rejectForm = reactive({ reason: '' })
const assignForm = reactive({ workerId: null as number | null, remark: '' })

// ---- fetch ----
async function fetchOrders() {
  loading.value = true
  try {
    const res = await getAdminOrders({
      page: filters.page,
      pageSize: filters.pageSize,
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
    })
    const data = res.data.data
    orders.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadWorkers() {
  const res = await getWorkerList()
  workers.value = res.data.data
}

function search() {
  filters.page = 1
  fetchOrders()
}

// ---- actions ----
function openApprove(row: RepairOrder) {
  currentOrder.value = row
  approveForm.priority = 'NORMAL'
  approveForm.remark = ''
  approveVisible.value = true
}

async function doApprove() {
  submitting.value = true
  try {
    await approveOrder(currentOrder.value!.id, {
      priority: approveForm.priority,
      remark: approveForm.remark || undefined,
    })
    ElMessage.success('审核通过')
    approveVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

function openReject(row: RepairOrder) {
  currentOrder.value = row
  rejectForm.reason = ''
  rejectVisible.value = true
}

async function doReject() {
  if (!rejectForm.reason.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    await ElMessageBox.confirm('确认驳回该工单？', '确认操作', { type: 'warning' })
  } catch {
    return
  }
  submitting.value = true
  try {
    await rejectOrder(currentOrder.value!.id, { reason: rejectForm.reason.trim() })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

function openAssign(row: RepairOrder) {
  currentOrder.value = row
  assignForm.workerId = null
  assignForm.remark = ''
  loadWorkers()
  assignVisible.value = true
}

async function doAssign() {
  if (!assignForm.workerId) {
    ElMessage.warning('请选择维修人员')
    return
  }
  try {
    await ElMessageBox.confirm('确认分派该工单？', '确认操作', { type: 'info' })
  } catch {
    return
  }
  submitting.value = true
  try {
    await assignOrder(currentOrder.value!.id, {
      workerId: assignForm.workerId,
      remark: assignForm.remark || undefined,
    })
    ElMessage.success('分派成功')
    assignVisible.value = false
    fetchOrders()
  } finally {
    submitting.value = false
  }
}

async function openLogs(row: RepairOrder) {
  currentOrder.value = row
  logVisible.value = true
  const res = await getOrderLogs(row.id)
  logs.value = res.data.data
}

// ---- helpers ----
function formatTime(s: string) {
  if (!s) return ''
  return s.replace('T', ' ').substring(0, 16)
}

function statusTag(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: 'warning', PENDING_ASSIGN: 'warning', PENDING_ACCEPT: '',
    PROCESSING: '', COMPLETED: 'success', EVALUATED: 'success',
    REJECTED: 'danger', CANCELLED: 'info',
  }
  return map[s] || ''
}

function statusLabel(s: string) {
  const map: Record<string, string> = {
    PENDING_REVIEW: '待审核', PENDING_ASSIGN: '待分派', PENDING_ACCEPT: '待接单',
    PROCESSING: '处理中', COMPLETED: '已完成', EVALUATED: '已评价',
    REJECTED: '已驳回', CANCELLED: '已取消',
  }
  return map[s] || s
}

function priorityTag(p: string) {
  const map: Record<string, string> = { LOW: 'info', NORMAL: '', HIGH: 'warning', URGENT: 'danger' }
  return map[p] || ''
}

function priorityLabel(p: string) {
  const map: Record<string, string> = { LOW: '低', NORMAL: '普通', HIGH: '高', URGENT: '紧急' }
  return map[p] || p
}

onMounted(fetchOrders)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  flex-wrap: wrap;
}

.table-panel {
  padding: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
```

- [ ] **Step 2: Verify frontend compiles**

Run: `cd frontend && npm run typecheck 2>&1 | head -30`
Expected: No errors (or only pre-existing errors)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/AdminOrdersView.vue
git commit -m "feat: implement admin orders page with approve/reject/assign dialogs and status log drawer"
```

---

## Self-Review

**1. Spec coverage:** All 8 spec sections covered — JWT auth (Task 1,3), entities/mappers (Task 2), service with transactions (Task 4), controllers (Task 3,4), frontend API (Task 5), frontend page (Task 6). RequireAdmin() guard covers role check. OrderStatus.canMoveTo() covers state validation. @Transactional covers transaction boundaries.

**2. Placeholder scan:** No TBD, no TODO, no "implement later", no "add appropriate error handling". All steps have actual code.

**3. Type consistency:** DTO records (LoginRequest, ApproveRequest, etc.) match service method signatures. Frontend interfaces match backend entity shapes.
