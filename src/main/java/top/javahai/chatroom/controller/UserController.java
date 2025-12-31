package top.javahai.chatroom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.yaml.snakeyaml.events.Event;
import top.javahai.chatroom.config.JwtProperties;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.entity.GroupMsgContent;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.entity.vo.UserLoginVO;
import top.javahai.chatroom.service.GroupMsgContentService;
import top.javahai.chatroom.service.UserService;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.utils.JwtUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
    public RespBean userLogin(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        log.info("userLogin: userLoginDTO={}",userLoginDTO);
        HttpSession session = request.getSession();
        // 3. 取出刚才生成图片时存入的正确验证码
        String sessionCode = (String) session.getAttribute("verify_code");

        // 4. 获取用户输入的验证码
        String userCode = userLoginDTO.getCode();

        // 5. 进行校验
        if (sessionCode == null) {
            return RespBean.error("验证码已过期，请刷新重试");
        }
        // 忽略大小写比对 (比如输入 A 和 a 都可以)
        if (userCode == null || !sessionCode.equalsIgnoreCase(userCode)) {
            return RespBean.error("验证码错误");
        }

        // 6. 校验通过后，建议清除 Session 中的验证码，防止重复使用
        session.removeAttribute("verify_code");
        // 1. 调用 Service 校验用户名密码
        User user = userService.login(userLoginDTO);

        // 2. 生成 JWT
        Map<String, Object> claims = new HashMap<>();
        // 存入用户ID，Key 为 "userId"
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        claims.put(JwtClaimsConstant.USERNAME, user.getUsername());
        claims.put(JwtClaimsConstant.NICKNAME, user.getNickname());
        claims.put(JwtClaimsConstant.USERTYPEID, user.getUserTypeId());

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
        return userService.selectUser(id);
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



    @Resource
    private GroupMsgContentService groupMsgContentService;

    @GetMapping("/getAllGroupMsgContent")
    private List<GroupMsgContent> getAllGroupMsgContent(){
        return groupMsgContentService.queryAllByLimit(null,null);
    }

}
