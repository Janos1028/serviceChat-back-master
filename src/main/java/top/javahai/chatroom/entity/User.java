package top.javahai.chatroom.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author makejava
 * @since 2020-06-16 12:08:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Integer id;
    /**
     * 登录账号
     */
    private String username;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 密码
     */
    private String password;
    /**
     * 用户头像
     */
    private String userProfile;
    /**
     * 用户状态id
     */
    private Integer userStateId;
    /**
     * 是否可用
     */
    private Boolean isEnabled;
    /**
     * 是否被锁定
     */
    private Boolean isLocked;

    private Integer userTypeId;

    public void setEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public void setLocked(Boolean locked) {
        isLocked = locked;
    }
}
