package top.javahai.chatroom.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import top.javahai.chatroom.entity.Admin;
import top.javahai.chatroom.entity.dto.AdminLoginDTO;
import top.javahai.chatroom.handler.exception.AccountNotFoundException;
import top.javahai.chatroom.handler.exception.PasswordErrorException;
import top.javahai.chatroom.mapper.AdminMapper;
import top.javahai.chatroom.service.AdminService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * (Admin)表服务实现类
 *
 * @author makejava
 * @since 2020-06-16 11:35:58
 */
@Service("adminService")
public class AdminServiceImpl implements AdminService{
    @Resource
    private AdminMapper adminMapper;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public Admin queryById(Integer id) {
        return this.adminMapper.queryById(id);
    }

    /**
     * 查询多条数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    @Override
    public List<Admin> queryAllByLimit(int offset, int limit) {
        return this.adminMapper.queryAllByLimit(offset, limit);
    }

    /**
     * 新增数据
     *
     * @param admin 实例对象
     * @return 实例对象
     */
    @Override
    public Admin insert(Admin admin) {
        this.adminMapper.insert(admin);
        return admin;
    }

    /**
     * 修改数据
     *
     * @param admin 实例对象
     * @return 实例对象
     */
    @Override
    public Admin update(Admin admin) {
        this.adminMapper.update(admin);
        return this.queryById(admin.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Integer id) {
        return this.adminMapper.deleteById(id) > 0;
    }

    @Override
    public Admin login(AdminLoginDTO adminLoginDTO) {
        String username = adminLoginDTO.getUsername();
        String password = adminLoginDTO.getPassword();

        // 1. 根据用户名查询数据库
        Admin admin = adminMapper.getAdminByUsername(username); // 假设 Mapper 有这个方法

        // 2. 处理各种异常情况
        if (admin == null) {
            throw new AccountNotFoundException("账号不存在"); // 建议使用自定义异常
        }

        // 使用 MD5 验证密码
        // 将输入的密码进行 MD5 加密后与数据库中存储的密文比对
        String inputPass = DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));

        if (!inputPass.equals(admin.getPassword())) {
            throw new PasswordErrorException("密码错误");
        }

        return admin;
    }

}
