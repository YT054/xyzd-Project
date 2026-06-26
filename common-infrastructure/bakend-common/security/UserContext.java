package com.campus.team.security;

public class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser u = get();
        return u == null ? null : u.getUserId();
    }

    public static Long getAdminId() {
        LoginUser u = get();
        return u == null ? null : u.getAdminId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
