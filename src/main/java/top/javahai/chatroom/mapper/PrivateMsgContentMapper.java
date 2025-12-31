package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.javahai.chatroom.entity.PrivateMsgContent;

import java.util.List;

@Mapper
public interface PrivateMsgContentMapper {

    @Insert("INSERT INTO private_msg_content (from_id, to_id, content, create_time, message_type_id, conversation_id) " +
            "VALUES (#{fromId}, #{toId}, #{content}, #{createTime}, #{messageTypeId}, #{conversationId})")
    int insert(PrivateMsgContent msg);

    // 获取两人之间的历史聊天记录（关联发送者信息以便前端展示头像等）
    // 注意：这里连接了 user 表来获取发送者的昵称和头像
    @Select("SELECT m.*, u.nickname as fromName, u.user_profile as fromProfile " +
            "FROM private_msg_content m " +
            "LEFT JOIN user u ON m.from_id = u.id " +
            "WHERE ((m.from_id = #{userId1} AND m.to_id = #{userId2}) " +
            "OR (m.from_id = #{userId2} AND m.to_id = #{userId1})) " +
            "ORDER BY m.create_time ASC")
    List<PrivateMsgContent> getHistoryMsg(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);
}
