package top.javahai.chatroom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.events.Event;
import top.javahai.chatroom.config.JwtProperties;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.vo.UserLoginVO;
import top.javahai.chatroom.service.UserService;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.utils.JwtUtil;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * (User)表控制层
 *
 * @author makejava
 * @since 2020-06-16 11:37:09
 */
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private JwtProperties jwtProperties;
    /**
     * 普通用户登录
     */
    @PostMapping("/login")
    public RespBean userLogin(@RequestBody UserLoginDTO userLoginDTO) {
        // 1. 调用 Service 校验用户名密码
        User user = userService.login(userLoginDTO);

        // 2. 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        // 存入用户ID，Key 为 "userId"
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.NICKNAME, user.getNickname());

        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        // 3. 封装 VO
        UserLoginVO userLoginVO = new UserLoginVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                token
        );

        return RespBean.ok("登录成功", userLoginVO);
    }

    /**
     * 服务对象
     */
    @Resource
    private UserService userService;

    /**
     * 注册操作
     */
    @PostMapping("/register")
    public RespBean addUser(@RequestBody User user){
        if (userService.insert(user)==1){
            return RespBean.ok("注册成功！");
        }else{
            return RespBean.error("注册失败！");
        }
    }

    /**
     * 注册操作，检查用户名是否已被注册
     * @param username
     * @return
     */
    @GetMapping("/checkUsername")
    public Integer checkUsername(@RequestParam("username")String username){
        return userService.checkUsername(username);
    }

    /**
     * 注册操作，检查昵称是否已被注册
     * @param nickname
     * @return
     */
    @GetMapping("/checkNickname")
    public Integer checkNickname(@RequestParam("nickname") String nickname){
        return userService.checkNickname(nickname);
    }

    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("/selectOne")
    public User selectOne(Integer id) {
        return this.userService.queryById(id);
    }

    /**
     * @author luo
     * @param page  页数，对应数据库查询的起始行数
     * @param size  数据量，对应数据库查询的偏移量
     * @param keyword 关键词，用于搜索
     * @param isLocked  是否锁定，用于搜索
     * @return
     */
    @GetMapping("/")
    public RespPageBean getAllUserByPage(@RequestParam(value = "page",defaultValue = "1") Integer page,
                                         @RequestParam(value = "size",defaultValue = "10") Integer size,
                                         String keyword,Integer isLocked){
        log.info("getAllUserByPage: page={},size={},keyword={},isLocked={}",page,size,keyword,isLocked);
        return userService.getAllUserByPage(page,size,keyword,isLocked);
    }

    /**
     * 更新用户的锁定状态
     * @author luo
     * @param id
     * @param isLocked
     * @return
     */
    @PutMapping("/")
    public RespBean changeLockedStatus(@RequestParam("id") Integer id,@RequestParam("isLocked") Boolean isLocked){
      if (userService.changeLockedStatus(id,isLocked)==1){
          return RespBean.ok("更新成功！");
      }else {
          return RespBean.error("更新失败！");
      }
    }

    /**
     * 删除单一用户
     * @author luo
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public RespBean deleteUser(@PathVariable Integer id){
        if (userService.deleteById(id)){
            return RespBean.ok("删除成功！");
        }
        else{
            return RespBean.error("删除失败！");
        }
    }

    /**
     * 批量删除用户
     * @author luo
     * @param ids
     * @return
     */
    @DeleteMapping("/")
    public RespBean deleteUserByIds(Integer[] ids){
        if (userService.deleteByIds(ids)==ids.length){
            return RespBean.ok("删除成功！");
        }else{
            return RespBean.error("删除失败！");
        }
    }


}
