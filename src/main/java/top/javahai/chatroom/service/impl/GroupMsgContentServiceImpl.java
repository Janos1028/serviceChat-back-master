package top.javahai.chatroom.service.impl;

import top.javahai.chatroom.entity.GroupMsgContent;
import top.javahai.chatroom.mapper.GroupMsgContentMapper;
import top.javahai.chatroom.entity.RespPageBean;
import top.javahai.chatroom.service.GroupMsgContentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * (GroupMsgContent)表服务实现类
 *
 * @author makejava
 * @since 2020-06-17 10:51:13
 */
@Service("groupMsgContentService")
public class GroupMsgContentServiceImpl implements GroupMsgContentService {
    @Resource
    private GroupMsgContentMapper groupMsgContentMapper;

    /**
     * 通过ID查询单条数据
     *
     * @param id 主键
     * @return 实例对象
     */
    @Override
    public GroupMsgContent queryById(Integer id) {
        return this.groupMsgContentMapper.queryById(id);
    }

    /**
     * 查询多条数据
     *
     * @param offset 查询起始位置
     * @param limit 查询条数
     * @return 对象列表
     */
    @Override
    public List<GroupMsgContent> queryAllByLimit(Integer offset, Integer limit) {
        return this.groupMsgContentMapper.queryAllByLimit(offset, limit);
    }

    /**
     * 新增数据
     *
     * @param groupMsgContent 实例对象
     * @return 实例对象
     */
    @Override
    public GroupMsgContent insert(GroupMsgContent groupMsgContent) {
        this.groupMsgContentMapper.insert(groupMsgContent);
        return groupMsgContent;
    }

    /**
     * 修改数据
     *
     * @param groupMsgContent 实例对象
     * @return 实例对象
     */
    @Override
    public GroupMsgContent update(GroupMsgContent groupMsgContent) {
        this.groupMsgContentMapper.update(groupMsgContent);
        return this.queryById(groupMsgContent.getId());
    }

    /**
     * 通过主键删除数据
     *
     * @param id 主键
     * @return 是否成功
     */
    @Override
    public boolean deleteById(Integer id) {
        return this.groupMsgContentMapper.deleteById(id) > 0;
    }

    @Override
    public RespPageBean getAllGroupMsgContentByPage(Integer page, Integer size, String nickname, Integer type, Date[] dateScope) {
        if (page!=null&&size!=null){
            page=(page-1)*size;
        }
        List<GroupMsgContent> allGroupMsgContentByPage = groupMsgContentMapper.getAllGroupMsgContentByPage(page, size, nickname, type, dateScope);
        Long total= groupMsgContentMapper.getTotal(nickname, type, dateScope);
        RespPageBean respPageBean = new RespPageBean();
        respPageBean.setData(allGroupMsgContentByPage);
        respPageBean.setTotal(total);
        return respPageBean;
    }

    @Override
    public Integer deleteGroupMsgContentByIds(Integer[] ids) {
        return groupMsgContentMapper.deleteGroupMsgContentByIds(ids);
    }
}
