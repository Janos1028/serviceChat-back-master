package top.javahai.chatroom.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatContext {
    private Integer domainId;
    private UserInfo fromUser; // 发起方
    private UserInfo toUser;   // 接收方
    private String conversationId;
    private String serviceName;
}
