package top.javahai.chatroom.utils;

import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.javahai.chatroom.constant.RedisConstant;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.UserInfo;
import top.javahai.chatroom.mapper.UserMapper;

import java.util.concurrent.TimeUnit;

/**
 * 用户信息缓存工具类
 * 作用：统一管理 Redis 中用户信息的查询与回写
 */
@Component
public class UserCacheUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    // 缓存过期时间 (与你原有逻辑保持一致：10小时)
    private static final long EXPIRE_TIME = 12;
    private static final TimeUnit EXPIRE_UNIT = TimeUnit.HOURS;

    /**
     * 根据 ID 获取 UserInfo (优先查 Redis)
     * @param userId 用户ID
     * @return UserInfo 对象，如果不存在则返回 null
     */
    public UserInfo getUserInfo(Integer userId) {
        if (userId == null) return null;

        String redisKey = RedisConstant.USER_ONLINE + userId;
        String userJson = redisTemplate.opsForValue().get(redisKey);

        if (userJson != null) {
            // 1. 命中缓存
            return JSON.parseObject(userJson, UserInfo.class);
        } else {
            // 2. 未命中，查数据库
            UserInfo userInfo = userMapper.getUserInfoById(userId);

            if (userInfo != null) {
                // 3. 回写缓存
                String json = JSON.toJSONString(userInfo);
                redisTemplate.opsForValue().set(redisKey, json, EXPIRE_TIME, EXPIRE_UNIT);
            }
            return userInfo;
        }
    }

    /**
     * 辅助方法：如果业务层需要 User 对象而不是 UserInfo
     * (UserInfo 是 User 的子集/视图，通常用于展示头像昵称)
     */
    public User getUser(Integer userId) {
        UserInfo userInfo = getUserInfo(userId);
        if (userInfo == null) return null;

        // 将 UserInfo 转为 User (仅包含基本展示信息)
        User user = new User();
        user.setId(userInfo.getId());
        user.setUsername(userInfo.getUsername());
        user.setNickname(userInfo.getNickname());
        user.setUserProfile(userInfo.getUserProfile());
        user.setUserTypeId(userInfo.getUserTypeId());
        return user;
    }
}
