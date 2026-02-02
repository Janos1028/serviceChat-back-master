package top.javahai.chatroom.controller.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import top.javahai.chatroom.config.JwtProperties;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.GroupMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.dto.UserRegisterDTO;
import top.javahai.chatroom.entity.vo.UserCardVO;
import top.javahai.chatroom.entity.vo.UserLoginVO;
import top.javahai.chatroom.service.UserService;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.utils.JwtUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public RespBean userLogin(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        log.info("userLogin: userLoginDTO={}",userLoginDTO);
        // 1. 调用 Service 校验用户名密码
        User user = userService.login(userLoginDTO);

        // 2. 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        // 存入用户ID，Key 为 "userId"
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.NICKNAME, user.getNickname());
        claims.put(JwtClaimsConstant.USERTYPEID, user.getUserTypeId());
        if (user.getUserTypeId() == 1){
            claims.put(JwtClaimsConstant.SERVICE_DOMAIN_ID, user.getServiceDomainId());
        }


        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        // 3. 封装 VO
        UserLoginVO userLoginVO = new UserLoginVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getUserTypeId(),
                user.getUserProfile(),
                token,
                user.getServiceDomainId()
        );

        return RespBean.ok("登录成功", userLoginVO);
    }

    /**
     * 退出登录
     * @return
     */
    @GetMapping("/logout")
    public RespBean logout() {
        userService.logout();
        return RespBean.ok("退出登录成功");
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
    public RespBean register(@RequestBody UserRegisterDTO userRegisterDTO){
        userService.insert(userRegisterDTO);
        return RespBean.ok("注册成功");
    }

    /**
     * 注册操作，检查用户名是否已被注册
     * @param username
     * @return
     */
    @GetMapping("/checkUsername")
    public RespBean checkUsername(@RequestParam("username") String username) {
        Integer result = userService.checkUsername(username);
        if (result > 0) {
            return RespBean.error("用户名已存在");
        }
        return RespBean.ok("用户名可用", result);
    }


    /**
     * 注册操作，检查昵称是否已被注册
     * @param nickname
     * @return
     */
    @GetMapping("/checkNickname")
    public RespBean checkNickname(@RequestParam("nickname") String nickname) {
        Integer result = userService.checkNickname(nickname);
        if (result > 0) {
            return RespBean.error("昵称已存在");
        }
        return RespBean.ok("昵称可用", result);
    }


    /**
     * 通过主键查询单条数据
     *
     * @return 单条数据
     */
    @GetMapping("/getCard")
    public RespBean getCard() {
        User current = (User) BaseContext.getCurrent();
        UserCardVO userCardVO = userService.selectUser(current.getId());
        return RespBean.ok(userCardVO);
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
     * 修改用户状态
     * @param stateId
     * @return
     */
    @PostMapping("/supporter/changeUserState")
    public RespBean changeUserState(Integer stateId){
        userService.changeUserState(stateId);
        return RespBean.ok();
    }


}
