package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import top.javahai.chatroom.config.UserPrincipal;
import top.javahai.chatroom.entity.Message;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.utils.TuLingUtil;

import java.io.IOException;
import java.security.Principal;
import java.util.Date;

@Controller
public class WsController {

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private PrivateChatService privateChatService;

  @Autowired
  private UserMapper userMapper;

  @MessageMapping("/ws/chat")
  public void handlePrivateMessage(Principal principal, Message message) {
    try {
      System.out.println("【WS消息】收到消息请求...");

      // 简单写一下核心逻辑
      String fromUsername = principal.getName();
      User fromUser = userMapper.getUserByUsername(fromUsername);
      User toUser = userMapper.getUserByUsername(message.getTo());

      String conversationId = privateChatService.getActiveConversationId(fromUser.getId(), toUser.getId());
      if (conversationId != null) {
        PrivateMsgContent privateMsg = new PrivateMsgContent();
        privateMsg.setFromId(fromUser.getId());
        privateMsg.setToId(toUser.getId());
        privateMsg.setContent(message.getContent());
        privateMsg.setCreateTime(new Date());
        privateMsg.setMessageTypeId(message.getMessageTypeId() != null ? message.getMessageTypeId() : 1);
        privateMsg.setConversationId(conversationId);
        privateMsg.setFromName(fromUser.getNickname());
        privateMsg.setFromProfile(fromUser.getUserProfile());
        privateChatService.saveMsg(privateMsg);
      }

      message.setFrom(fromUsername);
      message.setFromNickname(fromUser.getNickname());
      message.setFromUserProfile(fromUser.getUserProfile());
      message.setCreateTime(new Date());
      if (message.getMessageTypeId() == null) {
        message.setMessageTypeId(1);
      }
      simpMessagingTemplate.convertAndSendToUser(toUser.getUsername(), "/queue/chat", message);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  /**
   * 机器人聊天
   */
  @MessageMapping("/ws/robotChat")
  public void handleRobotChatMessage(Principal principal, Message message) throws IOException {
    User user = ((UserPrincipal) principal).getUser();
    message.setFrom(user.getUsername());
    message.setCreateTime(new Date());
    message.setFromNickname(user.getNickname());
    message.setFromUserProfile(user.getUserProfile());

    String result = TuLingUtil.replyMessage(message.getContent());

    Message resultMessage = new Message();
    resultMessage.setFrom("瓦力");
    resultMessage.setCreateTime(new Date());
    resultMessage.setFromNickname("瓦力");
    resultMessage.setContent(result);

    simpMessagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/robot", resultMessage);
  }
}
