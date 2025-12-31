package top.javahai.chatroom.entity;

import java.io.Serializable;

/**
 * @author makejava
 * @since 2020-06-16 11:35:56
 */
public class Admin implements Serializable {
    private static final long serialVersionUID = -75235725571250857L;

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
     * 管理员头像
     */
    private String userProfile;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(String userProfile) {
        this.userProfile = userProfile;
    }
}
