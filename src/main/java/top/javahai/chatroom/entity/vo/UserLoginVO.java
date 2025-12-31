package top.javahai.chatroom.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginVO implements Serializable {
    private Integer id;
    private String username;
    private String nickname;
    private Integer userTypeId;
    private String userProfile;
    private String token;

}
