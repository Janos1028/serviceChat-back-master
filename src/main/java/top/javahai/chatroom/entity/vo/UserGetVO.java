package top.javahai.chatroom.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGetVO {
    private Integer id;
    private String username;
    private String nickname;
    private String userProfile;
    private Integer userStateId;
    private Integer userTypeId;
    private String supportServiceName;
    private Date lastMessageTime;
}
