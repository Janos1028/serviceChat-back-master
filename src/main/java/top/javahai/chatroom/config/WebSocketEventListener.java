package top.javahai.chatroom.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import top.javahai.chatroom.config.UserPrincipal;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.UserService;

import java.security.Principal;

/**
 * WebSocket 事件监听器
 * 用于监听用户上线（连接建立）和离线（断开连接）事件
 */
@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private UserService userService;

    /**
     * 监听连接建立事件
     * 当前端执行 connect() 成功后触发
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // 获取消息头访问器
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        // 获取认证时存入的 Principal (在 WebSocketConfig 中设置的 UserPrincipal)
        Principal principal = headerAccessor.getUser();

        if (principal instanceof UserPrincipal) {
            User user = ((UserPrincipal) principal).getUser();
            if (user != null) {
                logger.info("用户上线: " + user.getUsername());
                // 更新数据库状态为 [在线] 并广播通知
                userService.setUserStateToOn(user.getId());
            }
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

        if (principal instanceof UserPrincipal) {
            User user = ((UserPrincipal) principal).getUser();
            if (user != null) {
                logger.info("用户断开连接: " + user.getUsername());
                // 更新数据库状态为 [离线] 并广播通知
                userService.setUserStateToLeave(user.getId());
            }
        }
    }
}
