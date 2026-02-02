package top.javahai.chatroom.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisterDTO {
    private Long id;
    private String nickname;
    private String username;
    private String password;
    private Integer userTypeId;
    private Integer serviceDomainId;
    private List<Integer> serviceIds;
    private String userProfile;

}
