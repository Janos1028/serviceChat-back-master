package top.javahai.chatroom.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.javahai.chatroom.entity.SupportService;

import java.util.Date;
import java.util.List;

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
    private Integer serviceDomainId;
    // 该用户拥有的服务列表 (仅用于查询展示，非数据库字段)
    private List<SupportService> supportServices;
    private String supportServiceName;
    private Date lastMessageTime;
}
