package top.javahai.chatroom.config;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.utils.JwtUtil;

import java.util.List;

/**
 * @author Hai
 * @date 2020/6/16 - 23:31
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  @Autowired
  private JwtProperties jwtProperties;

  /**
   * 注册stomp站点
   * @param registry
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws/ep").setAllowedOrigins("*").withSockJS();
  }

  /**
   * 注册拦截"/topic","/queue"的消息
   * @param registry
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 1. 配置消息代理，用于广播和点对点
    registry.enableSimpleBroker("/topic","/queue");

    // 2. 【核心修复】配置应用前缀
    // 这样前端发送 /app/ws/chat 时，Spring 会自动去掉 /app，从而匹配到 WsController 的 /ws/chat
    registry.setApplicationDestinationPrefixes("/app");
  }

  /**
   * 配置客户端入站通道拦截器
   * 用于鉴权和设置 User
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // ... 保持原有鉴权逻辑不变 ...
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          String token = accessor.getFirstNativeHeader("Authorization");

          if (!StringUtils.hasText(token)) {
            List<String> tokenList = accessor.getNativeHeader("token");
            if (tokenList != null && !tokenList.isEmpty()) {
              token = tokenList.get(0);
            }
          }

          if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
          }

          if (StringUtils.hasText(token)) {
            try {
              Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
              User user = new User();
              user.setId(Integer.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString()));
              user.setUsername(claims.get(JwtClaimsConstant.USERNAME).toString());
              user.setNickname(claims.get(JwtClaimsConstant.NICKNAME).toString());
              // 设置 Principal
              accessor.setUser(new UserPrincipal(user));
            } catch (Exception e) {
              System.out.println("WebSocket Token 校验失败: " + e.getMessage());
              return null;
            }
          }
        }
        return message;
      }
    });
  }
}
