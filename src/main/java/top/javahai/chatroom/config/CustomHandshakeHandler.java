package top.javahai.chatroom.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import top.javahai.chatroom.entity.User;

import java.security.Principal;
import java.util.Map;

/**
 * 自定义 WebSocket 握手处理器
 * 作用：用 UserId 代替 Username 作为 WebSocket 连接的唯一标识
 */
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 1. 获取 Spring Security 认证过的原始 Principal
        Principal principal = request.getPrincipal();

        if (principal == null) {
            return null;
        }

        // 2. 提取 UserId
        // 假设你之前配置了 UserPrincipal，或者 principal 本身能强转获取 User 信息
        // 如果你的 Security 配置里 principal.getName() 本来就是 username
        // 我们需要想办法拿到 User 对象。通常 SecurityContext 已经把 User 放进去了。

        // 如果你的 principal 是 UserPrincipal 类型 (根据你之前的 WsController 推断)
        if (principal instanceof UserPrincipal) {
            User user = ((UserPrincipal) principal).getUser();
            return new StompPrincipal(user.getId().toString());
        }

        // 如果上面的不行，也可以尝试从 attributes 或 session 拿，或者直接强转 Authentication
        // 这里提供一个通用简单的 StompPrincipal 定义
        return principal;
    }

    /**
     * 内部类：简单的 Principal 实现，只存 ID
     */
    private static class StompPrincipal implements Principal {
        private String userId;

        public StompPrincipal(String userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return this.userId; // 这里返回 ID，不再是 Username
        }
    }
}
