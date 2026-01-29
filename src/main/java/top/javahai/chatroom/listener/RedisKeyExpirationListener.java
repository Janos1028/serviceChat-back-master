package top.javahai.chatroom.listener;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import top.javahai.chatroom.constant.RedisConstant;
import top.javahai.chatroom.entity.PrivateChatConversation;
import top.javahai.chatroom.mapper.PrivateChatConversationMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.service.impl.PrivateChatServiceImpl;

import static top.javahai.chatroom.constant.ConversationConstant.CLOSE_CONVERSATION;
import static top.javahai.chatroom.constant.PrivateChatMsg.CONFIRM;
import static top.javahai.chatroom.constant.RedisConstant.*;
@Slf4j
@Component
public class RedisKeyExpirationListener extends KeyExpirationEventMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RedisKeyExpirationListener.class);


    @Autowired
    private PrivateChatConversationMapper conversationMapper;
    @Autowired
    private PrivateChatService privateChatService;

    public RedisKeyExpirationListener(RedisMessageListenerContainer listenerContainer) {
        super(listenerContainer);
    }

    /**
     * 当 Redis 中有 Key 过期时，会触发此方法
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 1. 获取过期的 Key 名称 (例如: chat:rate:timeout:1024)
        String expiredKey = message.toString();
        if (expiredKey != null && expiredKey.startsWith(FIRST_RESPONSE_TIMEOUT_KEY)) {
            try {
                // 提取会话ID
                String conversationId = expiredKey.replace(FIRST_RESPONSE_TIMEOUT_KEY, "");
                logger.info("会话 {} 客服超时未响应，触发自动转接逻辑", conversationId);

                // 调用 Service 处理
                privateChatService.handleFirstResponseTimeout(conversationId);

            } catch (Exception e) {
                logger.error("会话超时转接处理异常", e);
            }
        }
        // 2. 过滤：只处理我们需要关注的 Key
        if (expiredKey != null && expiredKey.startsWith(RATE_KEY_PREFIX)) {
            try {
                // 3. 提取会话 ID
                String conversationId = expiredKey.replace(RATE_KEY_PREFIX, "");
                logger.info("会话 {} 评价超时，触发自动好评逻辑", conversationId);

                // 4. 【兜底检查】查询数据库，确认该会话是否真的还没有评分
                // 防止极端并发情况（比如用户卡在第 29分59秒 提交了，但 Redis 还没来得及删）
                Short conversationScore = conversationMapper.getScoreById(conversationId);

                if (conversationScore == null) {
                    // 5. 执行更新：自动给 5 分
                    conversationMapper.updateScore(conversationId, 5);
                    logger.info(">>> 已自动为会话 {} 添加默认 5 分好评", conversationId);
                } else {
                    logger.info(">>> 会话 {} 已有评分或不存在，跳过自动操作", conversationId);
                }

            } catch (Exception e) {
                logger.error("自动好评处理异常", e);
            }
        }

        // 监听自动结束 Key (auto_close:xxxx)
        if (expiredKey.startsWith(AUTO_CLOSE_KEY_PREFIX)) {
            // 解析 Key: auto_close:会话ID:消息ID
            String[] parts = expiredKey.split(":");
            if (parts.length >= 3) {
                String convId = parts[1];
                Integer msgId = Integer.valueOf(parts[2]); // 解析出消息ID

                log.info("【自动结单】触发! convId={}, msgId={}", convId, msgId);

                // 调用 Service 关闭会话
                // 关键点：把 msgId 传进去！这样 closeConversation 就能把这个 ID 的卡片更新为“已解决”
                privateChatService.closeConversation(
                        convId,
                        CLOSE_CONVERSATION,
                        msgId
                );
            }
        }
    }
}
