package top.javahai.chatroom.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCardVO {
    private Integer id;
    private String nickName;
    private String userProfile;
    private Integer userStateId;
    private Integer userTypeId;
}
