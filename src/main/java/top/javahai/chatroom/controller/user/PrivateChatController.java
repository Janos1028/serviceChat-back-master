package top.javahai.chatroom.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.PrivateChatService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user/private")
public class PrivateChatController {

    @Autowired
    private PrivateChatService privateChatService;

    /**
     * 开启会话
     * @param toId 对方的用户ID
     */
    @PostMapping("/start")
    public RespBean startConversation(@RequestParam Integer toId) {
        User currentUser = (User) BaseContext.getCurrent();
        return privateChatService.startConversation(currentUser.getId(), toId);
    }

    /**
     * 关闭会话
     * @param conversationId 会话ID
     */
    @PostMapping("/close")
    public RespBean closeConversation(@RequestParam String conversationId) {
        return privateChatService.closeConversation(conversationId);
    }

    /**
     * 获取与某个用户的历史聊天记录
     * @param toId 对方ID
     */
    @GetMapping("/history")
    public List<PrivateMsgContent> getHistoryMsg(@RequestParam Integer toId) {
        User currentUser = (User) BaseContext.getCurrent();
        return privateChatService.getHistoryMsg(currentUser.getId(), toId);
    }

    /**
     * 【新增】查询与目标用户的会话状态
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
}
