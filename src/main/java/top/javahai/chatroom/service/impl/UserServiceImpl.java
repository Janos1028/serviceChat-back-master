package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.javahai.chatroom.entity.dto.UserLoginDTO;
import top.javahai.chatroom.mapper.UserMapper;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.service.UserService;
import org.springframework.stereotype.Service;
import top.javahai.chatroom.utils.UserUtil;

import javax.annotation.Resource;
import java.util.List;

/**
 * (User)表服务实现类
 *
 * @author makejava
 * @since 2020-06-16 11:37:09
 */
@Service("userService")
public class UserServiceImpl implements UserService{
    @Resource
    private UserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 获取除了当前用户的所有user表的数据
     * @return
     */
    @Override
    public List<User> getUsersWithoutCurrentUser() {
        return userMapper.getUsersWithoutCurrentUser(UserUtil.getCurrentUser().getId());
    }
    /**
     * 设置用户当前状态为在线
     * @param id 用户id
     */
    @Override
    public void setUserStateToOn(Integer id) {
        userMapper.setUserStateToOn(id);
    }
    /**
     * 设置用户当前状态为离线
     * @param id 用户id
     */
    @Override
    public void setUserStateToLeave(Integer id) {
        userMapper.setUserStateToLeave(id);
    }

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public User queryById(Integer id) {
        return this.userMapper.queryById(id);
    }

    /**
     * 查询多条数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    @Override
    public List<User> queryAllByLimit(int offset, int limit) {
        return this.userMapper.queryAllByLimit(offset, limit);
    }

    /**
     * 新增数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    @Override
    public Integer insert(User user) {
        //对密码进行加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodePass = encoder.encode(user.getPassword());
        user.setPassword(encodePass);
        user.setUserStateId(2);
        user.setEnabled(true);
        user.setLocked(false);
        return  this.userMapper.insert(user);
    }

    /**
     * 修改数据
     *
     * @param user 实例对象
     * @return 实例对象
     */
    @Override
    public Integer update(User user) {
        return this.userMapper.update(user);
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
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
            page=(page-1)*size;//起始下标
        }
        //获取用户数据
        List<User> userList= userMapper.getAllUserByPage(page,size,keyword,isLocked);
        //获取用户数据的总数
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
            throw new UsernameNotFoundException("用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

}
