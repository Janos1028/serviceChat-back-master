package top.javahai.chatroom.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 私聊会话实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateChatConversation {
    private String id;
    private Integer userId1;
    private Integer userId2;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date endTime;
    private Boolean isActive; // true: 进行中, false: 已结束

}
