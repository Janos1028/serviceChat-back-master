package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.entity.PrivateChatConversation;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.vo.UserGetVO;
import top.javahai.chatroom.handler.exception.StartConversationFailedException;
import top.javahai.chatroom.mapper.PrivateChatConversationMapper;
import top.javahai.chatroom.mapper.PrivateMsgContentMapper;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.PrivateChatService;

import java.util.*;

@Service
public class PrivateChatServiceImpl implements PrivateChatService {

    @Autowired
    private PrivateChatConversationMapper conversationMapper;
    @Autowired
    private PrivateMsgContentMapper msgMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String SESSION_KEY_PREFIX = "chat:active_sessions:";

    @Override
    public RespBean startConversation(Integer fromId, Integer toId) {
        // 0. 【核心修改】安全校验：检查发起者的身份
        User fromUser = userMapper.queryById(fromId);
        // 安全校验：检查接收者的身份
        User toUser = userMapper.queryById(toId);
        if (toUser.getUserTypeId() != null && toUser.getUserTypeId() == 0) {
            throw new StartConversationFailedException("您的账号权限不足，无法主动开启会话");
        }
        if (fromUser.getUserTypeId() != null && fromUser.getUserTypeId() == 1) {
            throw new StartConversationFailedException("您的账号权限不足，无法主动开启会话");
        }

        // 1. 查库
        PrivateChatConversation active = conversationMapper.getActiveConversation(fromId, toId);
        String conversationId;

        if (active != null) {
            conversationId = active.getId();
        } else {
            // 创建新会话
            conversationId = UUID.randomUUID().toString();
            PrivateChatConversation newConv = new PrivateChatConversation();
            newConv.setId(conversationId);
            newConv.setUserId1(fromId);
            newConv.setUserId2(toId);
            newConv.setCreateTime(new Date());
            newConv.setIsActive(true);
            conversationMapper.insert(newConv);

            // 【核心修改】存入数据库！Type=4 代表会话开启
            saveAndPushSystemMessage(fromId, toId, conversationId, 4, "会话已开启");

            // 发送打招呼消息 (Type=1)
            sendGreetingMsg(fromId, toId, conversationId);
        }

        // 2. Redis 同步
        redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + fromId, toId.toString(), conversationId);
        redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + toId, fromId.toString(), conversationId);

        // 3. WS 状态通知
        sendWsStatus(fromId, toId, conversationId, "START");

        return RespBean.ok("会话已开启", conversationId);
    }

    @Override
    public RespBean closeConversation(String conversationId) {
        PrivateChatConversation conv = conversationMapper.queryById(conversationId);
        if (conv != null) {
            conv.setIsActive(false);
            conv.setEndTime(new Date());
            conversationMapper.update(conv);

            // 存入数据库！Type=5 代表会话结束
            saveAndPushSystemMessage(conv.getUserId1(), conv.getUserId2(), conversationId, 5, "会话已结束");

            // Redis 移除
            redisTemplate.opsForHash().delete(SESSION_KEY_PREFIX + conv.getUserId1(), conv.getUserId2().toString());
            redisTemplate.opsForHash().delete(SESSION_KEY_PREFIX + conv.getUserId2(), conv.getUserId1().toString());

            // WS 状态通知
            sendWsStatus(conv.getUserId1(), conv.getUserId2(), conversationId, "END");

            return RespBean.ok("会话已关闭");
        }
        return RespBean.error("会话不存在");
    }

    // ... (getAllActiveSessions, getHistoryMsg, saveMsg, getActiveConversationId 保持不变) ...
    @Override
    public Map<Object, Object> getAllActiveSessions(Integer userId) {
        return redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + userId);
    }
    @Override
    public List<PrivateMsgContent> getHistoryMsg(Integer userId1, Integer userId2) {
        return msgMapper.getHistoryMsg(userId1, userId2);
    }
    @Override
    public void saveMsg(PrivateMsgContent msg) {
        msgMapper.insert(msg);
    }
    @Override
    public String getActiveConversationId(Integer userId1, Integer userId2) {
        PrivateChatConversation conv = conversationMapper.getActiveConversation(userId1, userId2);
        return conv != null ? conv.getId() : null;
    }

    // --- 辅助方法 ---

    /**
     * 【核心逻辑】保存并推送系统消息
     * 1. 存入数据库：保证刷新后还在。
     * 2. 推送给双方：保证实时看到。
     */
    private void saveAndPushSystemMessage(Integer fromId, Integer toId, String convId, Integer type, String content) {
        // 1. 存入数据库 (保证刷新后存在)
        PrivateMsgContent sysMsg = new PrivateMsgContent();
        sysMsg.setFromId(fromId);
        sysMsg.setToId(toId);
        sysMsg.setContent(content);
        sysMsg.setCreateTime(new Date());
        sysMsg.setMessageTypeId(type); // 4=Start, 5=End
        sysMsg.setConversationId(convId);

        User u = userMapper.queryById(fromId);
        if(u != null) {
            sysMsg.setFromName(u.getNickname());
            sysMsg.setFromProfile(u.getUserProfile());
        }
        msgMapper.insert(sysMsg); // --- 写入 DB ---

        // 2. 准备用户信息的 Map (避免多次查库)
        User user1 = userMapper.getUserByUsername(userMapper.queryById(fromId).getUsername()); // 发起者
        User user2 = userMapper.getUserByUsername(userMapper.queryById(toId).getUsername());   // 接收者

        // 3. 推送给 User1 (发起者)
        // 【关键技巧】对于发起者来说，这条“会话结束”的消息应该显示在“和 User2”的窗口里。
        // 所以我们需要“伪装”这条消息是 User2 发来的 (from = user2.username)。
        Map<String, Object> msgFor1 = new HashMap<>();
        msgFor1.put("content", content);
        msgFor1.put("messageTypeId", type);
        msgFor1.put("createTime", new Date());
        msgFor1.put("from", user2.getUsername()); // <--- 必须有这个 from 字段，且要是对方的用户名
        msgFor1.put("fromNickname", user2.getNickname());
        msgFor1.put("fromProfile", user2.getUserProfile());

        simpMessagingTemplate.convertAndSendToUser(user1.getUsername(), "/queue/chat", msgFor1);

        // 4. 推送给 User2 (接收者)
        // 对于接收者，消息本来就是 User1 发起的，所以 from = user1.username
        Map<String, Object> msgFor2 = new HashMap<>();
        msgFor2.put("content", content);
        msgFor2.put("messageTypeId", type);
        msgFor2.put("createTime", new Date());
        msgFor2.put("from", user1.getUsername()); // <--- 必须有这个 from 字段
        msgFor2.put("fromNickname", user1.getNickname());
        msgFor2.put("fromProfile", user1.getUserProfile());

        simpMessagingTemplate.convertAndSendToUser(user2.getUsername(), "/queue/chat", msgFor2);
    }
    private void sendGreetingMsg(Integer fromId, Integer toId, String convId) {
        User fromUser = userMapper.queryById(fromId);
        User toUser = userMapper.getUserByUsername(userMapper.queryById(toId).getUsername());

        PrivateMsgContent greeting = new PrivateMsgContent();
        greeting.setFromId(fromId);
        greeting.setToId(toId);
        greeting.setContent("你好，我们开始聊天吧！");
        greeting.setCreateTime(new Date());
        greeting.setMessageTypeId(1);
        greeting.setConversationId(convId);
        greeting.setFromName(fromUser.getNickname());
        greeting.setFromProfile(fromUser.getUserProfile());

        msgMapper.insert(greeting);

        // --- 构造推送消息 Payload ---
        Map<String, Object> msg = new HashMap<>();
        msg.put("content", greeting.getContent());
        msg.put("messageTypeId", 1);
        msg.put("createTime", new Date());
        msg.put("from", fromUser.getUsername());
        msg.put("fromNickname", fromUser.getNickname());
        msg.put("fromProfile", fromUser.getUserProfile());
        // 【关键新增】必须带上 'to'，否则发送方前端收到后不知道这条消息属于跟谁的聊天
        msg.put("to", toUser.getUsername());

        // 1. 推送给接收方 (toUser)
        simpMessagingTemplate.convertAndSendToUser(toUser.getUsername(), "/queue/chat", msg);

        // 2. 【核心修复】同时也推送给发送方 (fromUser)
        // 这样你的前端就能实时收到这条自己发出的打招呼消息了
        simpMessagingTemplate.convertAndSendToUser(fromUser.getUsername(), "/queue/chat", msg);
    }

    private void sendWsStatus(Integer u1, Integer u2, String convId, String type) {
        User user1 = userMapper.queryById(u1);
        User user2 = userMapper.queryById(u2);

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", type);
        msg.put("conversationId", convId);
        msg.put("fromId", u1);
        msg.put("toId", u2);

        simpMessagingTemplate.convertAndSendToUser(user1.getUsername(), "/queue/chat/status", msg);
        simpMessagingTemplate.convertAndSendToUser(user2.getUsername(), "/queue/chat/status", msg);
    }
}
