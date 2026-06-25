package com.cityrepair.service;

import java.util.List;

public class UserContext {

    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();

    public static void set(UserInfo user) {
        CONTEXT.set(user);
    }

    public static UserInfo get() {
        return CONTEXT.get();
    }

    public static Long getUserId() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.userId() : null;
    }

    public static List<String> getRoles() {
        UserInfo user = CONTEXT.get();
        return user != null ? user.roles() : List.of();
    }

    public static boolean hasRole(String role) {
        return getRoles().contains(role);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record UserInfo(Long userId, String username, List<String> roles) {
    }
}
