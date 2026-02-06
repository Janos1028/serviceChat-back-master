package top.javahai.chatroom.controller.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.entity.Message;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.UserInfo;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.utils.UserCacheUtil;

import java.security.Principal;
import java.util.Date;
import java.util.Map;

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

      message.setFromUserProfile(fromUser.getUserProfile());
      message.setCreateTime(new Date());
      message.setFromId(fromId);
      message.setToId(toUserId);
      if (message.getMessageTypeId() == null) {
        message.setMessageTypeId(1);
      }


      if (fromUser.getUserTypeId() != null && fromUser.getUserTypeId().equals(1)) {

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
        // 发给普通用户，隐藏支撑人员的信息
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(toUserId), "/queue/chat", message);
        // 支撑人员自己的界面回显
        message.setFromNickname(fromUser.getNickname());
        message.setTo(toUserInfo.getUsername());
        message.setFrom(fromUser.getUsername());
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(fromId), "/queue/chat", message);
      }

      if (fromUser.getUserTypeId() != null && fromUser.getUserTypeId().equals(0)){
        message.setFromNickname(fromUser.getNickname());
        message.setFrom(fromUser.getUsername());
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(toUserId), "/queue/chat", message);
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(fromId), "/queue/chat", message);

      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @MessageMapping("/chat/read")
  public void handleReadReceipt(Principal principal, Map<String, Object> payload) {
    try {
      // 1. 获取当前操作人（谁读了消息）
      Integer currentUserId = Integer.parseInt(principal.getName());

      // 2. 查询当前用户身份 (为了区分逻辑)
      UserInfo currentUser = userCacheUtil.getUserInfo(currentUserId);
      if (currentUser == null) return;

      // --- 分支 A: 我是客服 (UserTypeId == 1) ---
      // 逻辑对应 PrivateChatController.updateMsgStateToRead
      if (Integer.valueOf(1).equals(currentUser.getUserTypeId())) {
        Object targetIdObj = payload.get("targetId");
        if (targetIdObj != null) {
          Integer targetId = Integer.parseInt(targetIdObj.toString());
          // 调用 Service 更新 (客服读了 targetId 发的消息)
          privateChatService.updateMsgStateToRead(targetId, currentUser.getId());
        }
      } else {
        // --- 分支 B: 我是普通用户 (UserTypeId != 1) ---
        // 逻辑对应 PrivateChatController.updateServiceMsgRead
        Object domainIdObj = payload.get("domainId");
        Object staffIdObj = payload.get("staffId"); // 可能为空

        if (domainIdObj != null) {
          Integer domainId = Integer.parseInt(domainIdObj.toString());
          Integer staffId = null;
          if (staffIdObj != null) {
            staffId = Integer.parseInt(staffIdObj.toString());
          }
          // 调用 Service 更新 (用户读了 domainId 下 staffId 的消息)
          privateChatService.updateServiceMsgRead(domainId, staffId, currentUser.getId());
        }
      }

      // 3. 转发回执给对方 (保持原有的回显逻辑)
      String toUser = (String) payload.get("to");
      if (toUser != null) {
        simpMessagingTemplate.convertAndSendToUser(toUser, "/queue/chat", payload);
      }

    } catch (Exception e) {
      log.error("处理已读回执异常", e);
    }
  }

}

