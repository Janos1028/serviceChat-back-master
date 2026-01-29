package top.javahai.chatroom.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.vo.ConversationStartVO;
import top.javahai.chatroom.entity.vo.UserPrivateMsgContentVO;
import top.javahai.chatroom.handler.exception.StartConversationFailedException;
import top.javahai.chatroom.service.PrivateChatService;

import java.util.List;
import java.util.Map;

import static top.javahai.chatroom.constant.ConversationConstant.CLOSE_CONVERSATION;
import static top.javahai.chatroom.constant.ConversationConstant.FIRST_RESPONSE_CONVERSATION;

@RestController
@RequestMapping("/user/private")
public class PrivateChatController {

    @Autowired
    private PrivateChatService privateChatService;

    /**
     * 启动会话
     *
     * @param serviceId
     * @return
     */
    @PostMapping("/start")
    public RespBean startConversation(@RequestParam Integer domainId, @RequestParam(required = false) Integer serviceId) {
        // 1. 校验参数
        if (serviceId == null || domainId == null) {
            throw new StartConversationFailedException("参数错误：请指定服务类型ID");
        }

        User currentUser = (User) BaseContext.getCurrent();


        // 2. 直接调用随机分配逻辑
        ConversationStartVO conversationStartVO = privateChatService.startServiceConversation(domainId, serviceId, currentUser.getId());
        return RespBean.ok("会话开启成功", conversationStartVO);
    }
    /**
     * 客服请求关闭会话
     *
     * @param conversationId
     * @return
     */
    @PostMapping("/requestClose")
    public RespBean requestClose(@RequestParam String conversationId, @RequestParam Short isActive) {
        return privateChatService.requestCloseConversation(conversationId,isActive);
    }

    /**
     * 客服点击“未解决”重新开启会话
     * @param conversationId
     * @return
     */
    @PostMapping("/confirmUnsolved")
    public RespBean confirmUnsolved(@RequestParam String conversationId, @RequestParam Integer messageId) {
        return privateChatService.reopenConversation(conversationId, messageId);
    }

    /**
     * 用户点击“已解决”后关闭会话
     *
     * @param conversationId 会话ID
     */
    @PostMapping("/close")
    public RespBean closeConversation(@RequestParam String conversationId, @RequestParam Integer messageId) {
        return privateChatService.closeConversation(conversationId, CLOSE_CONVERSATION, messageId);
    }

    /**
     * 评价会话
     *
     * @param conversationId
     * @param score
     * @return
     */
    @PostMapping("/submitScore")
    public RespBean submitScore(@RequestParam String conversationId, @RequestParam Short score) {
        if (score == null || score < 1 || score > 5) {
            return RespBean.error("评分必须在1-5之间");
        }
        try {
            privateChatService.submitScore(conversationId, score);
            return RespBean.ok("评价提交成功");
        } catch (Exception e) {
            return RespBean.error("评价提交失败：" + e.getMessage());
        }
    }

    /**
     * 支撑人员获取与某个用户的历史聊天记录
     *
     * @param toId 对方ID
     */
    @GetMapping("/supporterGetHistoryMsg")
    public RespBean supporterGetHistoryMsg(@RequestParam("toId") Integer toId,
                                                          @RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "15") Integer size) {
        User currentUser = (User) BaseContext.getCurrent();
        List<PrivateMsgContent> privateMsgContents = privateChatService.supporterGetHistoryMsg(currentUser.getServiceDomainId(), toId, currentUser.getId(), page, size);
        return RespBean.ok(privateMsgContents);
    }

    /**
     * 普通用户获取支撑服务相关历史聊天记录
     *
     * @return
     */
    @GetMapping("/getHistoryMsg")
    public RespBean getHistoryMsg(@RequestParam("serviceDomainId") Integer serviceDomainId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "20") Integer size) {
        User currentUser = (User) BaseContext.getCurrent();
        List<UserPrivateMsgContentVO> historyMsg = privateChatService.getHistoryMsg(currentUser.getId(), serviceDomainId,page,size);
        return RespBean.ok(historyMsg);
    }


    /**
     * 【新增】查询与目标用户的会话状态
     *
     * @param toId 对方ID
     * @return 如果有活跃会话返回会话ID，否则返回null或特定状态
     */
    @GetMapping("/status")
    public RespBean getConversationStatus(@RequestParam Integer toId) {
        User currentUser = (User) BaseContext.getCurrent();
        String conversationId = privateChatService.getActiveConversationId(currentUser.getId(), toId);

        if (conversationId != null) {
            // 返回 obj = conversationId, status = 200
            return RespBean.ok("会话进行中", conversationId);
        } else {
            // 返回 error 或者 obj = null
            return RespBean.error("无活跃会话");
        }
    }

    @GetMapping("/active_sessions")
    public RespBean getAllActiveSessions() {
        User currentUser = (User) BaseContext.getCurrent();
        Map<Object, Object> sessions = privateChatService.getAllActiveSessions(currentUser.getId());
        return RespBean.ok(sessions);
    }

    /**
     * 获取离线期间未读会话
     *
     * @return
     */
    @GetMapping("/getUnreadSenders")
    public RespBean getUnreadSenders() {
        User currentUser = (User) BaseContext.getCurrent();
        List<Integer> unreadSenders = privateChatService.getUnreadSenders(currentUser.getId());
        return RespBean.ok(unreadSenders);
    }

    @PostMapping("/updateMsgStateToRead")
    public RespBean updateMsgStateToRead(@RequestParam Integer fromId) {
        // 将 fromId 发给 currentUserId 的所有消息状态置为 1
        privateChatService.updateMsgStateToRead(fromId);
        return RespBean.ok();
    }

    /**
     * 【普通用户专用】标记某个服务域的消息为已读
     *
     * @param domainId 服务域ID (必填)
     * @param staffId  具体的客服ID (选填，如果正在聊天则传，列表点击不传)
     */
    @PostMapping("/updateServiceMsgRead")
    public RespBean updateServiceMsgRead(
            @RequestParam Integer domainId,
            @RequestParam(required = false) Integer staffId) {
        privateChatService.updateServiceMsgRead(domainId, staffId);
        return RespBean.ok();
    }

    @PostMapping("/transfer")
    public RespBean transfer(@RequestParam String conversationId,
                             @RequestParam Integer newServiceId,
                             @RequestParam Integer domainId) {

        privateChatService.transferConversation(conversationId, newServiceId, domainId, FIRST_RESPONSE_CONVERSATION);
        return RespBean.ok("转接成功");
    }
}
