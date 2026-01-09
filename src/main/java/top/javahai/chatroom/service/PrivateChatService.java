package top.javahai.chatroom.service;

import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.RespBean;
import java.util.List;
import java.util.Map;

public interface PrivateChatService {
    RespBean startConversation(Integer fromId, Integer toId);
    RespBean closeConversation(String conversationId);
    List<PrivateMsgContent> getHistoryMsg(Integer userId1, Integer userId2);
    void saveMsg(PrivateMsgContent msg);
    String getActiveConversationId(Integer userId1, Integer userId2);

    //获取某用户的所有活跃会话
    Map<Object, Object> getAllActiveSessions(Integer userId);
    // 获取某用户所有未读会话
    List<Integer> getUnreadSenders(Integer userId);

    void updateMsgStateToRead(Integer fromId, Integer toId);
}
