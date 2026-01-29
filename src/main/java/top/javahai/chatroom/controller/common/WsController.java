package top.javahai.chatroom.controller.common;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.constant.RedisConstant;
import top.javahai.chatroom.entity.Message;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.UserInfo;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.utils.UserCacheUtil;

import java.security.Principal;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static top.javahai.chatroom.constant.RedisConstant.FIRST_RESPONSE_TIMEOUT_KEY;
import static top.javahai.chatroom.constant.RedisConstant.SESSION_KEY_PREFIX;

@Slf4j
@RestController
public class WsController {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private PrivateChatService privateChatService;

  @Autowired
  private UserMapper userMapper;
  @Autowired
  private UserCacheUtil userCacheUtil;
  @Autowired
  private StringRedisTemplate stringRedisTemplate;
  @MessageMapping("/ws/chat")
  public void handlePrivateMessage(Principal principal, Message message) {
    try {
      // 1. 获取发送者 (From)
      Integer fromId = Integer.parseInt(principal.getName());
      UserInfo fromUser = userCacheUtil.getUserInfo(fromId);
      if (fromUser == null) return;
      // 2. 获取接收者 (To)
      // 前端传的是 ID 字符串
      Integer toUserId = Integer.parseInt(message.getTo());

      // --- Redis 缓存查询开始 ---
      UserInfo toUserInfo = userCacheUtil.getUserInfo(toUserId);
      if (toUserInfo == null) {
        System.err.println("发送失败：找不到接收者 " + toUserId);
        return;
      }
      // --- Redis 缓存查询结束 ---

      // 3. 检查或生成会话ID
      String conversationId = privateChatService.getActiveConversationId(fromUser.getId(), toUserId);

      if (conversationId != null) {
        PrivateMsgContent privateMsg = new PrivateMsgContent();
        privateMsg.setFromId(fromUser.getId());
        privateMsg.setToId(toUserId);
        privateMsg.setContent(message.getContent());
        privateMsg.setCreateTime(new Date());
        privateMsg.setMessageTypeId(message.getMessageTypeId() != null ? message.getMessageTypeId() : 1);
        privateMsg.setConversationId(conversationId);
        privateMsg.setFromName(fromUser.getNickname());
        privateMsg.setFromProfile(fromUser.getUserProfile());
        Integer serviceDomainId;
        if (message.getServiceDomainId() == null){
          serviceDomainId = fromUser.getServiceDomainId();
        }else {
          serviceDomainId = message.getServiceDomainId();
        }
        privateMsg.setState(0);
        privateMsg.setServiceDomainId(serviceDomainId);
        privateMsg.setServiceName(message.getServiceName());
        // 存库操作 (历史记录)
        privateChatService.saveMsg(privateMsg);

        message.setConversationId(conversationId);
      }

      // 4. 完善消息对象
      message.setFrom(fromUser.getUsername());
      message.setFromNickname(fromUser.getNickname());
      message.setFromUserProfile(fromUser.getUserProfile());
      message.setCreateTime(new Date());
      message.setFromId(fromId);
      if (message.getMessageTypeId() == null) {
        message.setMessageTypeId(1);
      }
      message.setTo(toUserInfo.getUsername());

      if (fromUser.getUserTypeId() != null && fromUser.getUserTypeId() == 1) {

        // 尝试获取会话ID (如果消息里没传，就查一下)
        String currentConvId = message.getConversationId();
        if (currentConvId == null) {
          Object value = stringRedisTemplate.opsForHash().get(SESSION_KEY_PREFIX + fromId, toUserId);
          currentConvId = value == null ? privateChatService.getActiveConversationId(fromId, Integer.parseInt(message.getTo())) : value.toString();
        }

        if (currentConvId != null) {
          String key = FIRST_RESPONSE_TIMEOUT_KEY + currentConvId;
          // 如果存在超时Key，说明是第一次回复，删除它
          Boolean hasKey = stringRedisTemplate.hasKey(key);
          if (Boolean.TRUE.equals(hasKey)) {
            stringRedisTemplate.delete(key);
            log.info("客服(ID:{}) 已响应会话 {}，移除超时倒计时", fromId, currentConvId);
          }
        }
      }

      // 5. 推送消息
      // 接收者：使用从 Redis/DB 拿到的 username
      simpMessagingTemplate.convertAndSendToUser(String.valueOf(toUserId), "/queue/chat", message);

      // 发送者回显：直接使用 principal 中的 username
      simpMessagingTemplate.convertAndSendToUser(String.valueOf(fromId), "/queue/chat", message);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

