package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.*;
import top.javahai.chatroom.entity.PrivateChatConversation;

@Mapper
public interface PrivateChatConversationMapper {

    // 插入新会话
    @Insert("INSERT INTO private_chat_conversation (id, user_id_1, user_id_2, create_time, is_active) " +
            "VALUES (#{id}, #{userId1}, #{userId2}, #{createTime}, #{isActive})")
    int insert(PrivateChatConversation conversation);

    // 更新会话状态（用于关闭会话）
    @Update("UPDATE private_chat_conversation SET end_time = #{endTime}, is_active = #{isActive} WHERE id = #{id}")
    int update(PrivateChatConversation conversation);

    // 根据ID查询
    @Select("SELECT * FROM private_chat_conversation WHERE id = #{id}")
    PrivateChatConversation queryById(String id);

    // 查询两人当前是否还有正在进行的会话
    @Select("SELECT * FROM private_chat_conversation " +
            "WHERE ((user_id_1 = #{userId1} AND user_id_2 = #{userId2}) " +
            "OR (user_id_1 = #{userId2} AND user_id_2 = #{userId1})) " +
            "AND is_active = 1 LIMIT 1")
    PrivateChatConversation getActiveConversation(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
}
