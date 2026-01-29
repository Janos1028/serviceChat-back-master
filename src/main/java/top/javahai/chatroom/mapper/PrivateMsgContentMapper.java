package top.javahai.chatroom.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.javahai.chatroom.entity.PrivateMsgContent;
import top.javahai.chatroom.entity.vo.UserPrivateMsgContentVO;

import java.util.Date;
import java.util.List;

@Mapper
public interface PrivateMsgContentMapper {

    void insert(PrivateMsgContent msg);

    // 获取两人之间的历史聊天记录（关联发送者信息以便前端展示头像等）
    // 注意：这里连接了 user 表来获取发送者的昵称和头像
    List<PrivateMsgContent> supporterGetHistoryMsg(@Param("serviceDomainId") Integer serviceDomainId , @Param("userId") Integer userId, @Param("currentId")Integer currentId);

    List<Integer> getUnreadSenders(Integer userId);

    // 将未读消息状态改为已读
    void updateMsgStateToRead(Integer fromId, Integer toId);
    void updateStateByDomain(@Param("domainId") Integer domainId, @Param("toId") Integer toId);

    List<UserPrivateMsgContentVO> getHistoryMsg(Integer userId, Integer serviceDomainId, Date startDate);

    List<Integer> getUnreadStaffIdsByDomain(Integer domainId, Integer currentUserId);

    void updateMessageState(Integer messageId, Integer state);

    PrivateMsgContent selectById(Integer messageId);
}
