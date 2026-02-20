package top.javahai.chatroom.service.impl;

import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.javahai.chatroom.config.RabbitMQConfig;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.*;
import top.javahai.chatroom.entity.vo.ConversationStartVO;
import top.javahai.chatroom.entity.vo.UserPrivateMsgContentVO;
import top.javahai.chatroom.handler.exception.ConverstaionNotFoundException;
import top.javahai.chatroom.handler.exception.StartConversationFailedException;
import top.javahai.chatroom.handler.exception.TransferConversationException;
import top.javahai.chatroom.mapper.PrivateChatConversationMapper;
import top.javahai.chatroom.mapper.PrivateMsgContentMapper;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.PrivateChatService;
import top.javahai.chatroom.utils.UserCacheUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static top.javahai.chatroom.constant.ConversationConstant.*;
import static top.javahai.chatroom.constant.PrivateChatMsg.*;
import static top.javahai.chatroom.constant.RedisConstant.*;

@Slf4j
@Service
public class PrivateChatServiceImpl implements PrivateChatService {

    @Autowired
    private PrivateChatConversationMapper conversationMapper;
    @Autowired
    private PrivateMsgContentMapper msgMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PrivateChatConversationMapper privateChatConversationMapper;
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private UserCacheUtil userCacheUtil;
    @Autowired
    private PrivateMsgContentMapper privateMsgContentMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public String getConversationId(Integer domainId, Integer fromId, Integer toId, Integer serviceId) {
        // 0. 安全校验：检查发起者的身份
        UserInfo fromUser = userCacheUtil.getUserInfo(fromId);
        // 安全校验：检查接收者的身份
        UserInfo toUser = userCacheUtil.getUserInfo(toId);
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
            newConv.setIsActive((short) 1);
            newConv.setServiceDomainId(domainId);
            newConv.setServiceId(serviceId);
            String serviceName = userMapper.getServiceNameById(serviceId);
            newConv.setServiceName(serviceName);
            conversationMapper.insert(newConv);

            ChatContext ctx = ChatContext.builder()
                    .domainId(domainId)
                    .fromUser(fromUser)
                    .toUser(toUser)
                    .conversationId(conversationId)
                    .serviceName(serviceName)
                    .build();

            // 存入数据库！Type=4 代表会话开启
            saveAndPushSystemMessage(ctx, 4, "会话已开启", READ);

            // 发送打招呼消息 (Type=1)
            sendGreetingMsg(ctx);
        }

        // 2. Redis 同步
        redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + fromId, toId.toString(), conversationId);
        redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + toId, fromId.toString(), conversationId);

        // 3. WS 状态通知
        sendWsStatus(fromId, toId, conversationId, "START", domainId, serviceId);

        if (toUser.getUserTypeId() != null && toUser.getUserTypeId() == 1) {
            // 1. 设置 Redis 标记
            stringRedisTemplate.opsForValue().set("task:first_response:" + conversationId, "1");

            // 2. 发送 MQ 延时消息
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "FIRST_RESPONSE");
            msg.put("conversationId", conversationId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE_NAME, RabbitMQConfig.ROUNTING_KEY_5M, msg);
        }
        return conversationId;
    }

    /**
     * 用户点击“已解决”
     *
     * @param conversationId
     * @param isActive
     * @param messageId
     * @return
     */
    @Override
    public RespBean closeConversation(String conversationId, Short isActive, Integer messageId) {
        PrivateChatConversation conv = conversationMapper.queryById(conversationId);
        if (conv == null) {
            return RespBean.error("会话不存在");
        }
        if (conv.getIsActive().equals(FORCE_END) || conv.getIsActive().equals(CLOSE_CONVERSATION)) {
            return RespBean.error("会话已结束");
        }
        if (messageId != null) {
            broadcastConvConfirm(conversationId, messageId, conv, CONFIRM);
        }
        conv.setIsActive(isActive);
        conv.setEndTime(new Date());
        conversationMapper.update(conv);

        UserInfo user1 = userCacheUtil.getUserInfo(conv.getUserId1());
        UserInfo user2 = userCacheUtil.getUserInfo(conv.getUserId2());

        ChatContext ctx = ChatContext.builder()
                .domainId(conv.getServiceDomainId())
                .fromUser(user1)
                .toUser(user2)
                .conversationId(conversationId)
                .serviceName(null) // 注意：conv里得有这个字段，或者传null
                .build();
        if (isActive == 4) {
            saveAndPushSystemMessage(ctx, 5, "支撑人员已经强制关闭会话，会话结束", READ);
        } else {
            saveAndPushSystemMessage(ctx, 5, "会话已结束", READ);
        }

        // Redis 移除
        redisTemplate.opsForHash().delete(SESSION_KEY_PREFIX + conv.getUserId1(), conv.getUserId2().toString());
        redisTemplate.opsForHash().delete(SESSION_KEY_PREFIX + conv.getUserId2(), conv.getUserId1().toString());

        // WS 状态通知
        sendWsStatus(conv.getUserId1(), conv.getUserId2(), conversationId, "END", null, null);

        // 发送自动好评延时消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "AUTO_RATING");
        msg.put("conversationId", conversationId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE_NAME, RabbitMQConfig.ROUNTING_KEY_30M, msg);

        PrivateMsgContent cardMsg = new PrivateMsgContent();
        cardMsg.setConversationId(conversationId);
        cardMsg.setServiceDomainId(conv.getServiceDomainId());

        cardMsg.setFromId(conv.getUserId1());
        cardMsg.setToId(conv.getUserId1());

        cardMsg.setMessageTypeId(6); // 6 = 评价卡片
        cardMsg.setContent("服务已结束，请对本次服务进行评价"); // 提示语
        cardMsg.setCreateTime(new Date()); // 时间就是当前结束时间
        cardMsg.setState(1); // 已读

        msgMapper.insert(cardMsg);

        UserPrivateMsgContentVO ratingMsg = new UserPrivateMsgContentVO();
        ratingMsg.setMessageTypeId(6);
        ratingMsg.setConversationId(conversationId);
        ratingMsg.setScore(null); // 刚结束，分数为空
        ratingMsg.setCreateTime(new Date());
        ratingMsg.setContent("服务已结束，请评价");


        // 推送给用户 (User1) - 注意：推送到 /queue/chat 频道，让它出现在消息列表里
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(conv.getUserId1()),
                "/queue/chat",
                ratingMsg
        );
        return RespBean.ok("会话已关闭");
    }

    @Override
    public Map<Object, Object> getAllActiveSessions(Integer userId) {
        return redisTemplate.opsForHash().entries(SESSION_KEY_PREFIX + userId);
    }

    @Override
    public List<Integer> getUnreadSenders(Integer userId) {
        return msgMapper.getUnreadSenders(userId);
    }

    /**
     * 支撑人员界面的更新消息状态为已读
     *
     * @param fromId
     */
    @Override
    public void updateMsgStateToRead(Integer fromId,Integer currentUserId) {

        // 1. 数据库更新：将 fromId 发给 currentUserId 的消息标记为已读
        msgMapper.updateMsgStateToRead(fromId, currentUserId);

        // 2. 发送回执：告诉 fromId (用户)，我 (客服) 已经读了
        sendReadReceipt(currentUserId, fromId);
    }

    /**
     * 【场景B】用户端专用：更新服务域消息为已读
     *
     * @param domainId  服务域ID (必填)
     * @param staffId   具体的客服ID (选填)
     * @param currentId
     */
    @Override
    public void updateServiceMsgRead(Integer domainId, Integer staffId, Integer currentId) {

        if (staffId != null) {
            // === 情况1：精确更新 (前端明确知道是哪个客服) ===
            // 场景：在聊天窗口中，或者收到了具体客服的新消息

            // 1. 数据库更新：仅更新该客服发给我的消息
            msgMapper.updateMsgStateToRead(staffId, currentId);

            // 2. 发送回执：必须发，让该客服知道已读
            sendReadReceipt(currentId, staffId);

        } else {
            // === 情况2：批量已读 (逻辑大改) ===

            // 1. 【核心修正】先查出名单：谁在这个域下给我发了未读消息？
            // 必须在更新数据库之前查，否则更新完就查不到了(state都变1了)
            List<Integer> staffIds = privateMsgContentMapper.getUnreadStaffIdsByDomain(domainId, currentId);

            // 2. 更新数据库 (将该域下所有消息置为已读)
            privateMsgContentMapper.updateStateByDomain(domainId, currentId);

            // 3. 【遍历发送】给名单里的每一位客服发送回执
            if (staffIds != null && !staffIds.isEmpty()) {
                for (Integer targetStaffId : staffIds) {
                    // 只有列表里的人才需要通知，精准且全面
                    sendReadReceipt(currentId, targetStaffId);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferConversation(String conversationId, Integer newServiceId, Integer domainId, Short isActive) {
        // 1. 获取当前会话信息
        PrivateChatConversation oldConv = privateChatConversationMapper.queryById(conversationId);
        if (oldConv == null || oldConv.getIsActive() != 1) {
            throw new ConverstaionNotFoundException("当前会话无效或已结束");
        }
        if (!isActive.equals(CLOSE_CONVERSATION)) {
            throw new TransferConversationException("状态参数异常");
        }
        // 2. 识别身份：谁是普通用户？
        // 我们的规则是：userId1 是发起方(普通用户)，userId2 是接收方(客服)
        // 但为了保险，我们可以通过 userMapper 查一下 userTypeId
        Integer targetUserId = oldConv.getUserId1();
        Integer currentStaffId = oldConv.getUserId2(); // 默认假设 id2 是客服
        UserInfo user1 = userCacheUtil.getUserInfo(oldConv.getUserId1());
        if (user1.getUserTypeId() == 1) {
            // 如果 userId1 是客服，那 userId2 才是普通用户 (防止数据存反)
            currentStaffId = oldConv.getUserId1();
            targetUserId = oldConv.getUserId2();
        }
        List<Integer> supporterList = userMapper.getOnlineSupporterByServiceId(domainId, newServiceId);
        if (supporterList == null) {
            supporterList = new ArrayList<>();
        }

        // 必须排除掉自己（防止把自己当成新客服，导致转接死循环）
        if (currentStaffId != null) {
            // 注意：List<Integer> remove 需要传对象，否则会当成索引
            supporterList.remove(currentStaffId);
        }

        // 如果排除自己后没人了，直接抛出异常！
        // 此时，closeConversation 还没执行，WS 消息还没发，用户端毫无感知，体验完美。
        if (supporterList.isEmpty()) {
            throw new TransferConversationException("目标服务团队暂无其他在线人员，转接失败");
        }
        // 3. 结束当前会话 (通知旧客服和用户)
        // 注意：这里我们调用 closePrivateChat，它会推送 END 消息。
        // 用户端收到 END 后会显示"本次服务结束"，紧接着下面我们会推送新的 START，用户体验上就是"转接中..."
        this.closeConversation(conversationId, isActive, null);
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Integer selectedStaffId = supporterList.get(0);
        getConversationId(domainId, targetUserId, selectedStaffId, newServiceId);
        // 4. 开启新会话 (核心：以普通用户的名义，去匹配 newServiceId)
        // this.startPrivateChatForUser(domainId, newServiceId, targetUserId, supporterList);
    }

    @Override
    public void submitScore(String conversationId, Short score) {
        // 1. 校验会话是否存在
        PrivateChatConversation conv = conversationMapper.queryById(conversationId);
        if (conv == null) {
            throw new RuntimeException("会话不存在");
        }
        if (conv.getScore() != null){
            throw new RuntimeException("该会话已评分");
        }

        // 2. 更新数据库评分
        conversationMapper.updateScore(conversationId, score);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleFirstResponseTimeout(String conversationId) {
        PrivateChatConversation privateChatConversation = conversationMapper.queryById(conversationId);
        if (privateChatConversation == null) {
            throw new TransferConversationException("会话不存在");
        }
        if (privateChatConversation.getIsActive() != 1) {
            throw new TransferConversationException("会话已结束");
        }
        Integer serviceId = privateChatConversation.getServiceId();
        Integer serviceDomainId = privateChatConversation.getServiceDomainId();

        // --- 1. 修正身份识别逻辑 ---
        Integer id1 = privateChatConversation.getUserId1();
        Integer id2 = privateChatConversation.getUserId2();

        Integer customerId; // 普通用户 ID
        Integer staffId;    // 客服人员 ID

        // 这里的逻辑是：找出谁是 Type=1 (客服)
        UserInfo info1 = userCacheUtil.getUserInfo(id1);
        if (info1.getUserTypeId() != null && info1.getUserTypeId() == 1) {
            staffId = id1;
            customerId = id2;
        } else {
            customerId = id1;
            staffId = id2;
        }

        UserInfo customerUser = userCacheUtil.getUserInfo(customerId);
        UserInfo staffUser = userCacheUtil.getUserInfo(staffId);

        try {
            // A. 给【用户】发 (仅用户可见)
            sendPrivateSystemMessage(
                    serviceDomainId,
                    customerUser,
                    staffUser.getId(),
                    conversationId,
                    "当前对话调度人员超时未响应，已为您分配其他调度人员进行支撑。"
            );

            // B. 给【旧客服】发 (仅客服可见)
            sendPrivateSystemMessage(
                    serviceDomainId,
                    staffUser,
                    customerUser.getId(),
                    conversationId,
                    "您超时未响应，系统已分配其他调度人员进行支撑。"
            );
            // 尝试执行转接
            this.transferConversation(conversationId, serviceId, serviceDomainId, FIRST_RESPONSE_CONVERSATION);
        } catch (Exception e) {

            log.error("超时转接失败，执行强制关闭。ID: {}, 原因: {}", conversationId, e.getMessage());

            String failReason = "当前暂无其他支撑人员在线";

            // 4.1 给【用户】发失败通知 (覆盖/补充前面的安抚消息)
            sendPrivateSystemMessage(
                    serviceDomainId,
                    customerUser,
                    staffUser.getId(),
                    conversationId,
                    String.format("很抱歉，%s，服务自动结束。请稍后重试。", failReason)
            );


            // 5. 强制关闭
            try {
                this.closeConversation(conversationId, FIRST_RESPONSE_CONVERSATION, null);
            } catch (Exception ex) {
                log.error("关闭会话异常", ex);
            }
        }
    }

    @Override
    public RespBean requestCloseConversation(String conversationId, Short isActive) {
        PrivateChatConversation conv = conversationMapper.queryById(conversationId);
        if (conv == null) {
            return RespBean.error("会话不存在");
        }
        if (conv.getIsActive() != 1) {
            return RespBean.error("会话正在请求结束");
        }
        // 1. 修改会话状态
        conv.setIsActive(isActive);
        conversationMapper.update(conv);

        // 2. 获取双方信息
        UserInfo staff = userCacheUtil.getUserInfo(conv.getUserId2());
        UserInfo customer = userCacheUtil.getUserInfo(conv.getUserId1());

        ChatContext ctx = ChatContext.builder()
                .domainId(conv.getServiceDomainId())
                .fromUser(staff)
                .toUser(customer)
                .conversationId(conversationId)
                .serviceName(conv.getServiceName())
                .build();

        // 3. 推送系统交互消息 (MessageTypeId = 7)
        // 这个消息类型 7 专门给前端用来渲染“已解决/未解决”按钮
        PrivateMsgContent sysMsg = saveAndPushSystemMessage(ctx, 7, "客服已发起服务结束申请，请确认您的问题是否已解决？", WAITING);

        Integer msgId = sysMsg.getId();
        if (msgId != null) {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", "AUTO_CLOSE");
            msg.put("conversationId", conversationId);
            msg.put("messageId", msgId);
            rabbitTemplate.convertAndSend(RabbitMQConfig.DELAY_EXCHANGE_NAME, RabbitMQConfig.ROUNTING_KEY_30M, msg);
            log.info("【自动结单】已发送 MQ 倒计时, convId={}", conversationId);
        }


        return RespBean.ok("已发起结束服务申请，等待用户确认");
    }

    @Override
    public RespBean reopenConversation(String conversationId, Integer messageId) {
        PrivateChatConversation conv = conversationMapper.queryById(conversationId);
        if (conv == null) {
            return RespBean.error("会话不存在");
        }
        if (conv.getIsActive().equals(FORCE_END) || conv.getIsActive().equals(CLOSE_CONVERSATION)) {
            return RespBean.error("会话已结束");
        }
        if (conv.getIsActive().equals(WAITING_FOR_USER_CONFIRM)) {
            if (messageId != null) {
                broadcastConvConfirm(conversationId, messageId, conv, REJECT);
            }
            // 1. 状态恢复为 1 (进行中)
            conv.setIsActive(START_CONVERSATION);
            conversationMapper.update(conv);

            // 2. 通知客服
            UserInfo customer = userCacheUtil.getUserInfo(conv.getUserId1());
            UserInfo staff = userCacheUtil.getUserInfo(conv.getUserId2());

            ChatContext ctx = ChatContext.builder()
                    .domainId(conv.getServiceDomainId())
                    .fromUser(customer)
                    .toUser(staff)
                    .conversationId(conversationId)
                    .serviceName(null)
                    .build();

            saveAndPushSystemMessage(ctx, 5, "用户反馈问题未解决，会话继续", READ);
            return RespBean.ok("会话已恢复");
        }
        return RespBean.error("会话状态异常");
    }

    private void broadcastConvConfirm(String conversationId, Integer messageId, PrivateChatConversation conv, Integer state) {

        String key = AUTO_CLOSE_KEY_PREFIX + conversationId + ":" + messageId;
        stringRedisTemplate.delete(key);

        msgMapper.updateMessageState(messageId, state);
        PrivateMsgContent targetMsg = new PrivateMsgContent();
        targetMsg.setId(messageId);
        targetMsg.setConversationId(conversationId);
        targetMsg.setMessageTypeId(7);
        targetMsg.setState(state);
        targetMsg.setContent("客服已发起服务结束申请，请确认您的问题是否已解决？");
        targetMsg.setCreateTime(new Date());

        UserInfo user = userCacheUtil.getUserInfo(conv.getUserId1());
        UserInfo staff = userCacheUtil.getUserInfo(conv.getUserId2());

        // 广播给前端 -> 前端收到 id 和 state=3 -> 卡片变灰
        broadcastMessageUpdate(targetMsg, conv.getServiceDomainId(), user, staff);
    }

    /**
     * 发送单向可见的系统消息
     * 解决双重推送问题，确保只有目标用户能看到
     */
    private void sendPrivateSystemMessage(Integer domainId, UserInfo targetUser, Integer otherUserId,String convId, String content) {
        // 1. 存入数据库
        // 关键点：From 和 To 都是自己。
        // 这样对方查历史记录(Where from=对方 or to=对方)时，查不到这条消息。
        PrivateMsgContent sysMsg = new PrivateMsgContent();
        sysMsg.setFromId(targetUser.getId());
        sysMsg.setToId(targetUser.getId());
        sysMsg.setContent(content);
        sysMsg.setCreateTime(new Date());
        sysMsg.setMessageTypeId(5); // 5 = 系统提示
        sysMsg.setConversationId(convId);
        sysMsg.setServiceDomainId(domainId);
        sysMsg.setState(1); // 默认已读
        msgMapper.insert(sysMsg);

        // 2. 构造推送消息
        Map<String, Object> msg = new HashMap<>();
        msg.put("content", content);
        msg.put("messageTypeId", 5);
        msg.put("createTime", new Date());
        msg.put("conversationId", convId);
        msg.put("userTypeId",targetUser.getUserTypeId());
        // 填充必要字段，防止前端报错
        msg.put("from", targetUser.getUsername());
        msg.put("to", targetUser.getUsername());
        msg.put("fromId", targetUser.getId());
        msg.put("toId", targetUser.getId());
        if (targetUser.getUserTypeId() == 0){
            msg.put("serviceDomainId", domainId);
        }else {
            msg.put("otherUserId", otherUserId);
        }
        // 3. 只推送一次！
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(targetUser.getId()),
                "/queue/chat",
                msg
        );
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConversationStartVO startServiceConversation(Integer domainId, Integer serviceId, Integer userId) {
        // 3. 调用原有的开启会话逻辑，此时 toId 变为具体的员工ID
        int count = userMapper.isHasActiveConversationInDomain(domainId, userId);
        if (count > 0) {
            throw new StartConversationFailedException("当前服务中心已有正在进行的会话，请先结束当前服务后再开启新的会话");
        }
        // 1. 获取该服务分类下所有在线的支撑人员
        List<Integer> supporterList = userMapper.getOnlineSupporterByServiceId(domainId, serviceId);
        if (supporterList == null || supporterList.isEmpty()) {
            throw new StartConversationFailedException("抱歉，当前该服务团队暂无在线人员，请稍后再试");
        }

        // 2. 选择当前会话最少以及在线状态的支撑人员
        Integer selectedStaffId = supporterList.get(0);
        String conversationId = getConversationId(domainId, userId, selectedStaffId, serviceId);

        ConversationStartVO conversationStartVO = new ConversationStartVO();
        conversationStartVO.setConversationId(conversationId);
        conversationStartVO.setDomainId(domainId);
        conversationStartVO.setUserId(selectedStaffId);
        return conversationStartVO;
    }


    @Override
    public List<UserPrivateMsgContentVO> getHistoryMsg(Integer userId, Integer serviceDomainId, Integer page, Integer size) {
        // 1. 计算时间：获取3天前的时间点
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -3);
        Date startDate = calendar.getTime();

        PageHelper.startPage(page, size);

        List<UserPrivateMsgContentVO> list = msgMapper.getHistoryMsg(userId, serviceDomainId, startDate);

        // 只需要做这一件事：反转顺序
        Collections.reverse(list);


        return list;
    }

    @Override
    public List<PrivateMsgContent> supporterGetHistoryMsg(Integer serviceDomainId, Integer userId, Integer currentId, Integer page, Integer size) {
        // 1. 开启分页
        PageHelper.startPage(page, size);

        // 2. 查询 (SQL里限制了3天 + 倒序)
        List<PrivateMsgContent> privateMsgContentsList = msgMapper.supporterGetHistoryMsg(serviceDomainId, userId, currentId);


        // 4. 【反转】把顺序颠倒回来，变成“旧 -> 新”，让前端正常渲染
        Collections.reverse(privateMsgContentsList);
        return privateMsgContentsList;
    }


    @Override
    public void saveMsg(PrivateMsgContent msg) {
        msgMapper.insert(msg);
    }

    @Override
    public String getActiveConversationId(Integer userId1, Integer userId2) {
        Object cachedId = redisTemplate.opsForHash().get(SESSION_KEY_PREFIX + userId1, userId2.toString());
        if (cachedId != null) return cachedId.toString();

        // 2. 查库
        PrivateChatConversation conv = conversationMapper.getActiveConversation(userId1, userId2);

        if (conv != null) {
            // 3. 【关键】查到后回填 Redis，防止下条消息继续穿透 DB
            redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + userId1, userId2.toString(), conv.getId());
            redisTemplate.opsForHash().put(SESSION_KEY_PREFIX + userId2, userId1.toString(), conv.getId());
            return conv.getId();
        }
        return null;
    }


    /**
     * 广播消息状态更新
     * 告诉双方：某条消息的状态变了
     */
    private void broadcastMessageUpdate(PrivateMsgContent msg, Integer domainId, UserInfo user, UserInfo staff) {
        // 构造基础消息包
        Map<String, Object> updateMsg = new HashMap<>();
        updateMsg.put("id", msg.getId());              // 必须传 ID，前端靠这个匹配
        updateMsg.put("conversationId", msg.getConversationId());
        updateMsg.put("messageTypeId", msg.getMessageTypeId()); // 保持类型为 7
        updateMsg.put("content", msg.getContent());
        updateMsg.put("createTime", msg.getCreateTime());
        updateMsg.put("state", msg.getState());        // 【关键】最新的状态 (3 或 4)
        updateMsg.put("serviceDomainId", domainId);
        // --- A. 推送给用户 ---
        Map<String, Object> msgForUser = new HashMap<>(updateMsg);
        // 伪装发送者（保持和列表里的一致）
        msgForUser.put("from", "service_" + domainId);
        msgForUser.put("to", user.getUsername());
        // 必须保留原始发送者ID等信息，防止前端逻辑出错
        msgForUser.put("fromId", staff.getId());
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(user.getId()), "/queue/chat", msgForUser
        );

        // --- B. 推送给客服 ---
        Map<String, Object> msgForStaff = new HashMap<>(updateMsg);
        msgForStaff.put("from", user.getUsername()); // 设为用户，定位到正确的会话
        msgForStaff.put("fromId", user.getId());     // 设为用户ID
        msgForStaff.put("to", staff.getUsername());
        msgForStaff.put("userNickName", user.getNickname());
        // 客服端需要知道这条消息当初是“谁”发的，这里保持原样即可，前端只更新 state
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(staff.getId()), "/queue/chat", msgForStaff
        );
    }

    /**
     * 【核心逻辑】保存并推送系统消息
     * 1. 存入数据库：保证刷新后还在。
     * 2. 推送给双方：保证实时看到。
     */
    private PrivateMsgContent saveAndPushSystemMessage(ChatContext ctx, Integer type, String content, Integer state) {
        Integer domainId = ctx.getDomainId();
        UserInfo fromUser = ctx.getFromUser();
        UserInfo toUser = ctx.getToUser();
        String convId = ctx.getConversationId();
        String serviceName = ctx.getServiceName();

        // 1. 存入数据库 (保证刷新后存在)
        PrivateMsgContent sysMsg = new PrivateMsgContent();
        sysMsg.setFromId(fromUser.getId());
        sysMsg.setToId(toUser.getId());
        sysMsg.setContent(content);
        sysMsg.setCreateTime(new Date());
        sysMsg.setMessageTypeId(type); // 4=Start, 5=End
        sysMsg.setConversationId(convId);
        sysMsg.setServiceDomainId(domainId);
        sysMsg.setState(state);
        msgMapper.insert(sysMsg); // --- 写入 DB ---

        // 3. 推送给 User1 (接收者用户)
        Map<String, Object> msgFor1 = new HashMap<>();
        msgFor1.put("content", content);
        msgFor1.put("messageTypeId", type);
        msgFor1.put("createTime", new Date());
        msgFor1.put("conversationId", convId);
        msgFor1.put("id", sysMsg.getId());
        msgFor1.put("state", state);
        msgFor1.put("serviceDomainId", domainId);
        msgFor1.put("fromId", fromUser.getId());
        msgFor1.put("to", toUser.getUsername());
        if (toUser.getUserTypeId() != null && toUser.getUserTypeId() == 0) {
            msgFor1.put("from", "service_" + domainId); // 虚拟账号
            msgFor1.put("fromNickname", serviceName);      // 统一昵称
        } else {
            msgFor1.put("from", fromUser.getUsername());
            msgFor1.put("fromNickname", fromUser.getNickname());
        }

        simpMessagingTemplate.convertAndSendToUser(String.valueOf(toUser.getId()), "/queue/chat", msgFor1);

        // 4. 推送给 User2 (发起者客服) - 客服可以看到真实的用户信息
        Map<String, Object> msgFor2 = new HashMap<>();
        msgFor2.put("content", content);
        msgFor2.put("messageTypeId", type);
        msgFor2.put("createTime", new Date());
        msgFor2.put("from", fromUser.getUsername()); // 真实用户账号
        msgFor2.put("fromNickname", fromUser.getNickname());
        msgFor2.put("fromProfile", fromUser.getUserProfile());
        msgFor2.put("conversationId", convId);
        msgFor2.put("state", state);
        msgFor2.put("id", sysMsg.getId());
        msgFor2.put("fromId", fromUser.getId());
        if (toUser.getUserTypeId() != null && toUser.getUserTypeId() == 0) {
            msgFor1.put("to", "service_" + domainId); // 虚拟账号
        } else {
            msgFor1.put("to", toUser.getUsername());
        }
        msgFor2.put("to", toUser.getUsername());
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(fromUser.getId()), "/queue/chat", msgFor2);
        return sysMsg;
    }

    /**
     * 发送打招呼消息
     * 逻辑调整为：由被分配的客服（staffId）向用户（userId）发送欢迎语
     */
    private void sendGreetingMsg(ChatContext ctx) {
        Integer domainId = ctx.getDomainId();
        UserInfo user = ctx.getFromUser(); // 在start流程中，from通常是用户
        UserInfo staff = ctx.getToUser();  // 在start流程中，to通常是客服
        String convId = ctx.getConversationId();
        String serviceName = ctx.getServiceName();

        // 查询双方信息
        // 1. 定制专业的欢迎语内容
        String content = "您好，您已接入人工服务。这里是" + serviceName + "服务团队，很高兴为您服务~";

        // 2. 创建消息对象 (注意方向：From Staff -> To User)
        PrivateMsgContent greeting = new PrivateMsgContent();
        greeting.setFromId(staff.getId());  // 【修改】发送者是客服
        greeting.setToId(user.getId());     // 【修改】接收者是用户
        greeting.setContent(content);
        greeting.setCreateTime(new Date());
        greeting.setMessageTypeId(1); // 普通文本消息
        greeting.setConversationId(convId);
        greeting.setState(0); // 初始状态未读
        greeting.setServiceDomainId(domainId);

        // 存入数据库
        msgMapper.insert(greeting);

        // 2. 构造基础消息
        Map<String, Object> baseMsg = new HashMap<>();
        baseMsg.put("content", content);
        baseMsg.put("messageTypeId", 1);
        baseMsg.put("createTime", new Date());
        baseMsg.put("conversationId", convId);

        // --- A. 推送给用户 (必须匿名) ---
        Map<String, Object> msgForUser = new HashMap<>(baseMsg);
        msgForUser.put("to", user.getUsername());
        // 篡改发送者信息
        msgForUser.put("from", "service_" + domainId); // 虚拟账号
        msgForUser.put("fromNickname", serviceName + "团队");      // 统一昵称
        // 保留 fromId (真实ID)，用于前端发送已读回执 (ID本身不包含隐私)
        msgForUser.put("fromId", staff.getId());

        simpMessagingTemplate.convertAndSendToUser(String.valueOf(user.getId()), "/queue/chat", msgForUser);

        // --- B. 推送给客服 (显示真实，让自己知道是自己发的) ---
        Map<String, Object> msgForStaff = new HashMap<>(baseMsg);
        msgForStaff.put("to", user.getUsername());
        msgForStaff.put("from", staff.getUsername()); // 真实账号
        msgForStaff.put("fromNickname", staff.getNickname());
        msgForStaff.put("fromProfile", staff.getUserProfile());

        simpMessagingTemplate.convertAndSendToUser(String.valueOf(staff.getId()), "/queue/chat", msgForStaff);
    }

    private void sendWsStatus(Integer u1, Integer u2, String convId, String type, Integer domainId, Integer serviceId) {
        UserInfo user1 = userCacheUtil.getUserInfo(u1);
        UserInfo user2 = userCacheUtil.getUserInfo(u2);
        String serviceName = null;
        if (serviceId != null) {
            serviceName = userMapper.getServiceNameById(serviceId);
        }
        // 基础信息
        Map<String, Object> baseMsg = new HashMap<>();
        baseMsg.put("type", type);
        baseMsg.put("conversationId", convId);
        if (serviceId != null) baseMsg.put("serviceId", domainId);
        if (serviceName != null) {
            baseMsg.put("serviceName", serviceName);
        }
        // --- A. 发给用户 (隐藏客服信息) ---
        Map<String, Object> msgForUser = new HashMap<>(baseMsg);

        // 用户的“对手”是客服，这里必须篡改
        msgForUser.put("toId", u2);   // 客服ID (ID本身是安全的，且用于回执)
        // 【关键】告诉用户：你在跟 service_X 聊天
        if (serviceId != null) {
            msgForUser.put("toUsername", "service_" + serviceId); // 之前前端逻辑依赖这个字段匹配窗口
            // 注意：这里用 serviceId 或 domainId 对应的虚拟号都可以，取决于你前端 users 列表里的 username 是什么
            // 你的前端逻辑里，服务号是 service_domainId (如 service_1)
            // 如果 serviceId 就是 domainId，那就没问题。
            // 假如这里传入的 serviceId 是具体的业务线ID，可能需要确认一下。
            // 稳妥起见，如果 domainId 没传进来，这里可以用 serviceId 代替，或者再传一个 domainId 参数进来。
            // 根据你的 getConversationId 调用，最后一个参数传的是 serviceId (domainId)。所以这里是对的。
        }

        msgForUser.put("fromId", u1);
        msgForUser.put("fromUsername", user1.getUsername());

        simpMessagingTemplate.convertAndSendToUser(String.valueOf(u1), "/queue/chat/status", msgForUser);

        // --- B. 发给客服 (显示真实用户信息) ---
        Map<String, Object> msgForStaff = new HashMap<>(baseMsg);

        // 客服需要知道对方(用户)的真实信息
        msgForStaff.put("fromId", u1);
        msgForStaff.put("fromUsername", user1.getUsername());
        msgForStaff.put("fromNickname", user1.getNickname());
        msgForStaff.put("fromProfile", user1.getUserProfile());

        msgForStaff.put("toId", u2);
        msgForStaff.put("toUsername", user2.getUsername());

        simpMessagingTemplate.convertAndSendToUser(String.valueOf(u2), "/queue/chat/status", msgForStaff);
    }

    /**
     * 内部辅助方法：构造并发送已读回执
     *
     * @param readerId 谁读了消息（当前操作人，通常是接收者）
     * @param senderId 谁发了消息（消息发送者，回执接收人）
     */
    private void sendReadReceipt(Integer readerId, Integer senderId) {
        // 查询读者的信息（为了让发送者知道是谁读的）
        UserInfo reader = userCacheUtil.getUserInfo(readerId);

        if (reader != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "READ_RECEIPT"); // 消息类型：已读回执
            payload.put("readerId", readerId);
            payload.put("readerName", reader.getUsername()); // 或者用 nickname

            // 推送到消息发送者的状态频道
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(senderId),
                    "/queue/chat/status",
                    payload
            );
        }
    }
}
