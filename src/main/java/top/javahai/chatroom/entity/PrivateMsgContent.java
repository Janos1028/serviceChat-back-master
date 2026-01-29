package top.javahai.chatroom.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 私聊消息内容实体
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateMsgContent implements Serializable {
    private Integer id;
    private Integer fromId; // 发送者ID
    private Integer toId;   // 接收者ID
    private String content; // 消息内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;

    private Integer messageTypeId; // 消息类型
    private String conversationId; // 关联的会话ID
    private Integer state; // 消息状态，0:未读，1:已读
    private Integer serviceDomainId;
    private String serviceName;
    // 以下字段用于前端显示，数据库中无此字段
    private String fromName;
    private String fromProfile;

}
