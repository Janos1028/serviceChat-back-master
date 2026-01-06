package top.javahai.chatroom.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO {
    private String username;
    private String password;
    private String code;
    private String verifyKey; // 验证码在 Redis 中的 Key
}
