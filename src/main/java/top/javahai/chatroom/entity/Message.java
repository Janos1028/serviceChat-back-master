package top.javahai.chatroom.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
  private String from;
  private String to;
  private String content;
  private Date createTime;
  private String fromNickname;
  private String fromUserProfile;
  private Integer messageTypeId;
  private String conversationId;
  private Integer fromId;
  private Integer serviceDomainId;
  private String serviceName;
  }
