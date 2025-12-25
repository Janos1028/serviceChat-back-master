package top.javahai.chatroom.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * (User)实体类
 *
 * @author makejava
 * @since 2020-06-16 12:08:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User  implements UserDetails {

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




    //账号是否未过期
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    //账号是否不锁定
    @Override
    public boolean isAccountNonLocked() {
        return  !isLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return  isEnabled;
    }


    /**
     * 获取用户拥有的所有角色
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }



    public void setEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public void setLocked(Boolean locked) {
        isLocked = locked;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", nickname='" + nickname + '\'' +
                ", password='" + password + '\'' +
                ", userProfile='" + userProfile + '\'' +
                ", userStateId=" + userStateId +
                ", isEnabled=" + isEnabled +
                ", isLocked=" + isLocked +
                '}';
    }
}
