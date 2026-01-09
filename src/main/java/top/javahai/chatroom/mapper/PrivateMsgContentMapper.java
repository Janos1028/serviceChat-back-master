package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.javahai.chatroom.entity.PrivateMsgContent;

import java.util.List;

@Mapper
public interface PrivateMsgContentMapper {

    void insert(PrivateMsgContent msg);

    // 获取两人之间的历史聊天记录（关联发送者信息以便前端展示头像等）
    // 注意：这里连接了 user 表来获取发送者的昵称和头像
    List<PrivateMsgContent> getHistoryMsg(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    List<Integer> getUnreadSenders(Integer userId);

    // 将未读消息状态改为已读
    void updateMsgStateToRead(Integer fromId, Integer toId);
}
