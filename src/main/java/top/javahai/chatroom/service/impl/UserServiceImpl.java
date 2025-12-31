package top.javahai.chatroom.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.vo.UserGetVO;
import top.javahai.chatroom.handler.exception.AccountNotFoundException;
import top.javahai.chatroom.handler.exception.PasswordErrorException;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.UserService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (User)表服务实现类
 *
 * @author makejava
 * @since 2020-06-16 11:37:09
 */
@Slf4j
@Service("userService")
public class UserServiceImpl implements UserService{
    @Resource
    private UserMapper userMapper;

    // 【新增】注入消息模板，用于发送 WebSocket 消息
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 获取除了当前用户的所有user表的数据
     *
     * @return
     */
    @Override
    public List<UserGetVO> getUsersWithoutCurrentUser() {
        User user = (User)BaseContext.getCurrent();
        Integer userTypeId = user.getUserTypeId();
        if (userTypeId == 1) {
            // 当前用户是咨询人，可以获取所有用户
            List<UserGetVO> userGetVOS = userMapper.ConstantGetUsersWithoutCurrentUser(user.getId());
            log.info("获取到的用户："+userGetVOS);
            return userGetVOS;
        }else {
            // 当前用户是普通用户，只能获取咨询人
            return userMapper.getUsersWithoutCurrentUser(user.getId());
        }
    }
    /**
     * 设置用户当前状态为在线
     * @param id 用户id
     */
    @Override
    public void setUserStateToOn(Integer id) {
        userMapper.setUserStateToOn(id);
        // 【新增】广播用户上线消息
        broadcastUserStatus(id, 1); // 1 代表在线
    }
    /**
     * 设置用户当前状态为离线
     * @param id 用户id
     */
    @Override
    public void setUserStateToLeave(Integer id) {
        userMapper.setUserStateToLeave(id);
        // 【新增】广播用户离线消息
        broadcastUserStatus(id, 2); // 2 代表离线
    }
    /**
     * 【新增】辅助方法：广播用户状态变更
     * 推送目标：/topic/userStatus (所有订阅了该主题的客户端都能收到)
     */
    private void broadcastUserStatus(Integer userId, Integer statusId) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userStateId", statusId);
        // 发送到公共主题，前端订阅此主题即可
        simpMessagingTemplate.convertAndSend("/topic/userStatus", map);
    }

    @Override
    public UserGetVO queryById(Integer id) {
        return this.userMapper.queryById(id);
    }

    @Override
    public List<User> queryAllByLimit(int offset, int limit) {
        return this.userMapper.queryAllByLimit(offset, limit);
    }

    @Override
    public Integer insert(User user) {
        // 使用 MD5 加密
        // 注意：这里使用的是 Spring 工具类 org.springframework.util.DigestUtils
        String encodePass = DigestUtils.md5DigestAsHex(user.getPassword().getBytes(StandardCharsets.UTF_8));
        user.setPassword(encodePass);

        user.setUserStateId(2);
        user.setEnabled(true);
        user.setLocked(false);
        return  this.userMapper.insert(user);
    }

    @Override
    public Integer update(User user) {
        return this.userMapper.update(user);
    }

    @Override
    public boolean deleteById(Integer id) {
        return this.userMapper.deleteById(id) > 0;
    }

    @Override
    public Integer checkUsername(String username) {
        return userMapper.checkUsername(username);
    }

    @Override
    public Integer checkNickname(String nickname) {
        return userMapper.checkNickname(nickname);
    }

    @Override
    public RespPageBean getAllUserByPage(Integer page, Integer size,String keyword,Integer isLocked) {
        if (page!=null&&size!=null){
            page=(page-1)*size;
        }
        List<User> userList= userMapper.getAllUserByPage(page,size,keyword,isLocked);
        Long total= userMapper.getTotal(keyword,isLocked);
        RespPageBean respPageBean = new RespPageBean();
        respPageBean.setData(userList);
        respPageBean.setTotal(total);
        return respPageBean;
    }

    @Override
    public Integer changeLockedStatus(Integer id, Boolean isLocked) {
        return userMapper.changeLockedStatus(id,isLocked);
    }

    @Override
    public Integer deleteByIds(Integer[] ids) {
        return userMapper.deleteByIds(ids);
    }

    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        User user = userMapper.getUserByUsername(username);
        if (user == null){
            throw new AccountNotFoundException("用户不存在");
        }

        // 使用 MD5 验证密码
        // 将输入的密码进行 MD5 加密后与数据库中存储的密文比对
        String inputPass = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (!inputPass.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误");
        }
        userMapper.setUserStateToOn(user.getId());
        return user;
    }

    @Override
    public User selectUser(Integer id) {
        return userMapper.selectUser(id);

    }
}
