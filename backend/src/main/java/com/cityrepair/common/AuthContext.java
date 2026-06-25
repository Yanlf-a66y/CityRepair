package com.cityrepair.common;

public final class AuthContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(CurrentUser user) { HOLDER.set(user); }
    public static CurrentUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }

    public record CurrentUser(Long userId, String username, String role) {}
}
