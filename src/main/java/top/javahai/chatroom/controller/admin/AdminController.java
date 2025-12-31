package top.javahai.chatroom.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import top.javahai.chatroom.config.JwtProperties;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.entity.Admin;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.dto.AdminLoginDTO;
import top.javahai.chatroom.entity.vo.AdminLoginVO;
import top.javahai.chatroom.service.AdminService;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.service.UserService;
import top.javahai.chatroom.utils.JwtUtil;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * (Admin)表控制层
 *
 * @author makejava
 * @since 2020-06-16 11:35:59
 */
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
    @Autowired
    private JwtProperties jwtProperties;
    /**
     * 服务对象
     */
    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;
    /**
     * 通过主键查询单条数据
     *
     * @param id 主键
     * @return 单条数据
     */
    @GetMapping("/selectOne")
    public Admin selectOne(Integer id) {
        return this.adminService.queryById(id);
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public RespBean adminLogin(@RequestBody AdminLoginDTO adminLoginDTO) {
        log.info("管理员登录：{}", adminLoginDTO);

        // 1. 调用 Service 进行登录校验 (需要在 Service 中实现 login 方法)
        Admin admin = adminService.login(adminLoginDTO);

        // 2. 登录成功后，生成 JWT 令牌
        Map<String, Object> claims = new HashMap<>();

        claims.put(JwtClaimsConstant.ADMIN_ID, admin.getId());
        claims.put(JwtClaimsConstant.USERNAME, admin.getUsername());
        claims.put(JwtClaimsConstant.NICKNAME, admin.getNickname());

        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        // 3. 封装返回结果 VO
        AdminLoginVO adminLoginVO = new AdminLoginVO(
                admin.getId(),
                admin.getUsername(),
                admin.getNickname(),
                admin.getUserProfile(),
                token
        );

        return RespBean.ok("登录成功", adminLoginVO);
    }

    @GetMapping("/getAllUserByPage")
    public RespPageBean getAllUserByPage(@RequestParam(value = "page",defaultValue = "1") Integer page,
                                         @RequestParam(value = "size",defaultValue = "10") Integer size,
                                         String keyword, Integer isLocked){
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
    @PutMapping("/changeLockedStatus")
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
    @DeleteMapping("/deleteUser/{id}")
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
    @DeleteMapping("/deleteUserByIds")
    public RespBean deleteUserByIds(Integer[] ids){
        if (userService.deleteByIds(ids)==ids.length){
            return RespBean.ok("删除成功！");
        }else{
            return RespBean.error("删除失败！");
        }
    }
}
