package top.javahai.chatroom.service;

import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.vo.ConversationStartVO;
import top.javahai.chatroom.entity.vo.UserPrivateMsgContentVO;

import java.util.List;
import java.util.Map;

public interface PrivateChatService {
    RespBean closeConversation(String conversationId, Short isActive, Integer messageId);
    List<PrivateMsgContent> supporterGetHistoryMsg(Integer serviceDomainId, Integer userId, Integer currentId, Integer page, Integer size);
    void saveMsg(PrivateMsgContent msg);
    String getActiveConversationId(Integer userId1, Integer userId2);

    //获取某用户的所有活跃会话
    Map<Object, Object> getAllActiveSessions(Integer userId);
    // 获取某用户所有未读会话
    List<Integer> getUnreadSenders(Integer userId);

    void updateMsgStateToRead(Integer fromId);

    ConversationStartVO startServiceConversation(Integer domainId, Integer serviceId, Integer userId);

    List<UserPrivateMsgContentVO> getHistoryMsg(Integer userId, Integer serviceDomainId, Integer page, Integer size);

    void updateServiceMsgRead(Integer domainId, Integer staffId);

    void transferConversation(String conversationId, Integer newServiceId, Integer domainId, Short isActive);

    void submitScore(String conversationId, Short score);

    void handleFirstResponseTimeout(String conversationId);

    RespBean requestCloseConversation(String conversationId, Short isActive);

    RespBean reopenConversation(String conversationId, Integer messageId);
}
