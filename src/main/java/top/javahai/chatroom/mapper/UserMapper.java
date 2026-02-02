package top.javahai.chatroom.mapper;

import top.javahai.chatroom.entity.ServiceDomain;
import top.javahai.chatroom.entity.SupportService;
import top.javahai.chatroom.entity.User;
import org.apache.ibatis.annotations.Param;
import top.javahai.chatroom.entity.UserInfo;
import top.javahai.chatroom.entity.dto.UserRegisterDTO;
import top.javahai.chatroom.entity.vo.UserCardVO;
import top.javahai.chatroom.entity.vo.UserGetVO;

import java.util.List;

public interface UserMapper {

    /**
     * 根据用户名查询用户对象
     * @param username
     * @return
     */
    User getUserByUsername(String username);



    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    User queryById(Integer id);

    /**
     * 查询指定行数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    List<User> queryAllByLimit(@Param("offset") int offset, @Param("limit") int limit);


    /**
     * 通过实体作为筛选条件查询
     *
     * @param user 实例对象
     * @return 对象列表
     */
    List<User> queryAll(User user);

    /**
     * 用户注册
     *
     * @param user 实例对象
     */
    Long register(UserRegisterDTO user);

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 影响行数
     */
    int update(User user);

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Integer id);

    void setUserStateToOn(Integer id);

    void setUserStateToLeave(Integer id);

    Integer checkUsername(String username);

    Integer checkNickname(String nickname);

    List<User> getAllUserByPage(@Param("page") Integer page, @Param("size") Integer size,String keyword,Integer isLocked);

    Long getTotal(@Param("keyword") String keyword,@Param("isLocked") Integer isLocked);

    Integer changeLockedStatus(@Param("id") Integer id, @Param("isLocked") Boolean isLocked);

  Integer deleteByIds(@Param("ids") Integer[] ids);

    List<UserGetVO> ConstantGetUsersWithoutCurrentUser(Integer id);

    UserCardVO selectUser(Integer id);

    List<UserGetVO> getRecentConversation(Integer serviceDomainId, Integer currentUserId);

    /**
     * 获取所有唯一的支撑服务分类
     * @return
     */
    List<SupportService> getSupportServiceCategories(Integer domainId);

    /**
     * 获取指定服务下的所有在线的客服
     * @param serviceId
     * @return
     */
    List<Integer> getOnlineSupporterByServiceId(Integer domainId, @Param("serviceId") Integer serviceId);

    /**
     * 获取所有服务域（大类）
     */
    List<ServiceDomain> getAllServiceDomains();

    /**
     * 根据服务域ID获取该域下的支撑服务
     * @param domainId 服务域ID
     */
    List<SupportService> getSupportServicesByDomainId(@Param("domainId") Integer domainId);

    UserInfo getUserInfoById(Integer id);

    String getServiceNameById(Integer serviceId);

    void changeUserState(Integer id, Integer stateId);

    int isHasActiveConversationInDomain(Integer domainId, Integer userId);

    /**
     * 根据服务域ID获取服务域
     * @param domainId 服务域ID
     */
    String getDomainById(Integer domainId);

    void registerService(Long userId,Integer serviceId);
}
