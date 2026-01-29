package top.javahai.chatroom.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import top.javahai.chatroom.config.UserPrincipal;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.UserService;

import java.security.Principal;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 事件监听器
 * 用于监听用户上线（连接建立）和离线（断开连接）事件
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private UserService userService;
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /**
     * 监听连接建立事件
     * 当前端执行 connect() 成功后触发
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();

        // 【修改】直接使用通用的 getUserId 方法，它能兼容 StompPrincipal
        Integer userId = getUserId(principal);

        if (userId != null) {

            // 更新数据库状态为 [在线]
            userService.setUserStateToOn(userId);
        } else {
            logger.warn("用户上线，但无法获取有效 UserId");
        }
    }

    /**
     * 监听连接断开事件
     * 当用户关闭浏览器、刷新页面、网络中断或主动 disconnect 时触发
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();

        Integer userId = getUserId(principal);
        if (userId != null) {
            // 【修改核心】不要立即改数据库，而是延迟 2 秒执行
            scheduler.schedule(() -> {
                try {
                // 1. 去注册中心查一下，这个用户现在还有没有连接？
                // principal.getName() 在使用了 CustomHandshakeHandler 后就是 userId 字符串
                SimpUser simpUser = simpUserRegistry.getUser(userId.toString());

                // 2. 如果检测到还有 Session (说明是刷新页面，新连接已经连上了)
                if (simpUser != null && simpUser.hasSessions()) {
                    return; // 撤销，什么都不做
                }

                userService.setUserStateToLeave(userId);
                } catch (Exception e) {
                    logger.error("用户离线状态更新失败", e);
                }

            }, 2, TimeUnit.SECONDS); // 延迟 2 秒

        }
    }

    private Integer getUserId(Principal principal) {
        if (principal == null) {
            return null;
        }

        // 情况1: 假如 HandshakeHandler 没起作用，还是原始的 UserPrincipal
        if (principal instanceof UserPrincipal) {
            User user = ((UserPrincipal) principal).getUser();
            return user != null ? user.getId() : null;
        }

        // 情况2: 被 CustomHandshakeHandler 包装过，Principal.getName() 就是 ID 字符串
        try {
            return Integer.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            // 如果 name 不是纯数字（例如是 username），说明解析失败
            logger.warn("WebSocket Event: 无法从 Principal 解析 UserId: " + principal.getName());
            return null;
        }
    }
}
