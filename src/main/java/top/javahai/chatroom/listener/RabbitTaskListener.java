package top.javahai.chatroom.listener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javahai.chatroom.entity.PrivateChatConversation;
import top.javahai.chatroom.mapper.PrivateChatConversationMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.config.RabbitMQConfig;

import java.util.Map;

import static top.javahai.chatroom.constant.ConversationConstant.CLOSE_CONVERSATION;
import static top.javahai.chatroom.constant.ConversationConstant.WAITING_FOR_USER_CONFIRM;

@Slf4j
@Component
public class RabbitTaskListener {

    @Autowired
    private PrivateChatService privateChatService;
    @Autowired
    private PrivateChatConversationMapper conversationMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = RabbitMQConfig.PROCESS_QUEUE)
    public void handleDelayedTask(Map<String, Object> msg) {
        String type = (String) msg.get("type");
        String conversationId = (String) msg.get("conversationId");

        if ("FIRST_RESPONSE".equals(type)) {
            String flagKey = "task:first_response:" + conversationId;
            // 如果 Redis 里没有这个 Key，说明客服已回复，直接丢弃任务
            if (!stringRedisTemplate.hasKey(flagKey)) {
                log.info("会话 {} 客服已响应，取消首问超时转接", conversationId);
                return;
            }
            log.info("会话 {} 首问超时，执行转接", conversationId);
            privateChatService.handleFirstResponseTimeout(conversationId);
            stringRedisTemplate.delete(flagKey);

        } else if ("AUTO_RATING".equals(type)) {
            Short score = conversationMapper.getScoreById(conversationId);
            if (score == null) {
                conversationMapper.updateScore(conversationId, 5);
                log.info("已为会话 {} 自动好评", conversationId);
            }

        } else if ("AUTO_CLOSE".equals(type)) {
            Integer messageId = (Integer) msg.get("messageId");
            PrivateChatConversation conv = conversationMapper.queryById(conversationId);
            if (conv != null && WAITING_FOR_USER_CONFIRM.equals(conv.getIsActive())) {
                privateChatService.closeConversation(conversationId, CLOSE_CONVERSATION, messageId);
                log.info("会话 {} 等待确认超时，自动结单", conversationId);
            }
        }
    }
}
