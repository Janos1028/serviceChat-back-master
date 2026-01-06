package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.*;
import top.javahai.chatroom.entity.PrivateChatConversation;

@Mapper
public interface PrivateChatConversationMapper {

    // 插入新会话

    int insert(PrivateChatConversation conversation);

    // 更新会话状态（用于关闭会话）

    int update(PrivateChatConversation conversation);

    // 根据ID查询
    PrivateChatConversation queryById(String id);

    // 查询两人当前是否还有正在进行的会话
    PrivateChatConversation getActiveConversation(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
}
