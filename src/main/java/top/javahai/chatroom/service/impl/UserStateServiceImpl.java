package top.javahai.chatroom.service.impl;

import top.javahai.chatroom.entity.UserState;
import top.javahai.chatroom.mapper.UserStateMapper;
import top.javahai.chatroom.service.UserStateService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * (UserState)表服务实现类
 *
 * @author makejava
 * @since 2020-06-16 11:36:02
 */
@Service("userStateService")
public class UserStateServiceImpl implements UserStateService {
    @Resource
    private UserStateMapper userStateMapper;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public UserState queryById(Integer id) {
        return this.userStateMapper.queryById(id);
    }

    /**
     * 查询多条数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    @Override
    public List<UserState> queryAllByLimit(int offset, int limit) {
        return this.userStateMapper.queryAllByLimit(offset, limit);
    }

    /**
     * 新增数据
     *
     * @param userState 实例对象
     * @return 实例对象
     */
    @Override
    public UserState insert(UserState userState) {
        this.userStateMapper.insert(userState);
        return userState;
    }

    /**
     * 修改数据
     *
     * @param userState 实例对象
     * @return 实例对象
     */
    @Override
    public UserState update(UserState userState) {
        this.userStateMapper.update(userState);
        return this.queryById(userState.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Integer id) {
        return this.userStateMapper.deleteById(id) > 0;
    }
}
