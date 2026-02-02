package top.javahai.chatroom.service;

import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.ServiceDomain;
import top.javahai.chatroom.entity.SupportService;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.dto.UserRegisterDTO;
import top.javahai.chatroom.entity.vo.UserCardVO;
import top.javahai.chatroom.entity.vo.UserGetVO;

import java.util.List;

/**
 * (User)表服务接口
 *
 * @author makejava
 * @since 2020-06-16 11:37:09
 */
public interface UserService {




    /**
     * 设置用户当前状态为在线
     * @param id 用户id
     */
    void setUserStateToOn(Integer id);

    /**
     * 设置用户当前状态为离线
     * @param id
     */
    void setUserStateToLeave(Integer id);

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    UserGetVO queryById(Integer id);


    /**
     * 新增数据
     *
     * @param userRegisterDTO 实例对象
     * @return 实例对象
     */
    void insert(UserRegisterDTO userRegisterDTO);

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    Integer update(User user);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    boolean deleteById(Integer id);

    /**
     * 检查用户名是否已存在
     * @param username
     * @return
     */
    Integer checkUsername(String username);

    /**
     * 检查昵称是否存在
     * @param nickname
     * @return
     */
    Integer checkNickname(String nickname);

    RespPageBean getAllUserByPage(Integer page, Integer size,  String keyword,Integer isLocked);

    Integer changeLockedStatus(Integer id, Boolean isLocked);

    Integer deleteByIds(Integer[] ids);

    User login(UserLoginDTO userLoginDTO);

    UserCardVO selectUser(Integer id);

    List<UserGetVO> getRecentConversation(Integer serviceDomainId, Integer currentUserId);

    List<SupportService> getSupportServiceCategories(Integer domainId);

    List<ServiceDomain> getAllServiceDomains();

    void logout();

    void changeUserState(Integer stateId);
}
