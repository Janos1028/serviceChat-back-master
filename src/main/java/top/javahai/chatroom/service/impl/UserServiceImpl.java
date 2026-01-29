package top.javahai.chatroom.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.*;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.vo.UserCardVO;
import top.javahai.chatroom.entity.vo.UserGetVO;
import top.javahai.chatroom.handler.exception.*;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.service.UserService;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static top.javahai.chatroom.constant.RedisConstant.*;

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

    @Autowired
    private StringRedisTemplate redisTemplate;


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
        User user = userMapper.queryById(id);
        UserGetVO userGetVO = new UserGetVO();
        BeanUtils.copyProperties(user, userGetVO);
        return userGetVO;
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
        // 先校验验证码是否正确
        String verifyKey = userLoginDTO.getVerifyKey();
        String code = userLoginDTO.getCode();
        // 判断验证码是否为空
        if(StringUtils.isBlank(verifyKey) || StringUtils.isBlank(code)){
            throw new VerifycodeEmptyException("验证码不能为空");
        }
        // 从redis中获取验证码
        String redisKey = VERIFY_CODE_KEY + verifyKey;
        String redisCode = redisTemplate.opsForValue().get(redisKey);
        // 若redis中的验证码为空，则说明已过期，则抛出异常
        if(StringUtils.isBlank(redisCode)){
            throw new VerifycodeEmptyException("验证码已过期");
        }
        if(!redisCode.equalsIgnoreCase(code)){
            throw new VerifycodeErrorException("验证码错误");
        }
        // 验证通过后，立即删除 Redis 中的 Key，防止重复使用
        redisTemplate.delete(redisKey);

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
        this.setUserStateToOn(user.getId());
        if (redisTemplate.opsForValue().get(USER_ONLINE+user.getId())==null){
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(user, userInfo);
            String userJson = JSON.toJSONString(userInfo);
            redisTemplate.opsForValue().set(USER_ONLINE+user.getId(),userJson, 10 * 60 * 60, TimeUnit.SECONDS);
        }

        return user;
    }

    @Override
    public UserCardVO selectUser(Integer id) {
        return userMapper.selectUser(id);

    }

    @Override
    public List<UserGetVO> getRecentConversation(Integer serviceDomainId, Integer currentUserId) {
        return userMapper.getRecentConversation(serviceDomainId, currentUserId);
    }

    @Override
    public List<SupportService> getSupportServiceCategories(Integer domainId) {
        return userMapper.getSupportServiceCategories(domainId);
    }

    @Override
    public List<ServiceDomain> getAllServiceDomains() {
        String allServiceDomains = redisTemplate.opsForValue().get(ALL_SERVICE_DOMAINS_KEY);
        if (allServiceDomains != null) {
            return JSON.parseArray(allServiceDomains, ServiceDomain.class);
        }else {
            List<ServiceDomain> serviceDomains = userMapper.getAllServiceDomains();
            String json = JSON.toJSONString(serviceDomains);
            redisTemplate.opsForValue().set(ALL_SERVICE_DOMAINS_KEY, json, 2, TimeUnit.HOURS);
            return serviceDomains;
        }
    }

    @Override
    public void logout() {
        // 1. 从 BaseContext 获取当前登录用户（由 JwtTokenUserInterceptor 拦截器注入）
        User user = (User) BaseContext.getCurrent();

        if (user != null) {
            // 2. 更新数据库状态为 [离线] 并广播 WebSocket 消息
            // (复用你已有的 setUserStateToLeave 方法)
            this.setUserStateToLeave(user.getId());

            // 3. 立即清除 Redis 中的用户信息缓存，防止数据残留
            // 这里的 USER_ONLINE 是常量，对应 key 前缀
            redisTemplate.delete(USER_ONLINE + user.getId());

        }
    }

    @Override
    public void changeUserState(Integer stateId) {
        User currentUser = (User) BaseContext.getCurrent();
        if (currentUser.getUserTypeId() == 0){
            throw new UserChangeStateException("当前用户无法主动切换状态！");
        }
        this.userMapper.changeUserState(currentUser.getId(), stateId);
    }
}
