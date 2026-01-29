package top.javahai.chatroom.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPrivateMsgContentVO {
    private Integer id;
    private Integer fromId; // 发送者ID
    private Integer toId;   // 接收者ID
    private String content; // 消息内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    private Integer messageTypeId; // 消息类型
    private String conversationId; // 关联的会话ID
    private Integer state; // 消息状态，0:未读，1:已读

    private Short score;
    private String serviceName;

}
