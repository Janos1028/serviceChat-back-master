package top.javahai.chatroom.config;

import top.javahai.chatroom.entity.User;

import java.security.Principal;

/**
 * WebSocket 自定义 Principal
 * 用于在 WebSocket Session 中存储用户信息，替代 Spring Security 的 Principal
 */
public class UserPrincipal implements Principal {
    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }
}
