package top.javahai.chatroom.controller.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.ServiceDomain;
import top.javahai.chatroom.entity.SupportService;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.vo.UserGetVO;
import top.javahai.chatroom.service.UserService;

import java.util.List;



@Slf4j
@RestController
@RequestMapping("/user/chat")
public class ChatController {
  @Autowired
  private UserService userService;

  /**
   * 普通用户获取获取所有支撑服务类型
   * @return
   */
  @GetMapping("/getSupportServiceCategories")
  public RespBean getSupportServiceCategories(@RequestParam(required = false) Integer domainId){
    if (domainId==null){
      return RespBean.error("请选择服务域");
    }
    List<SupportService> supportServiceCategories = userService.getSupportServiceCategories(domainId);
    return RespBean.ok(supportServiceCategories);
  }

  /**
   * 获取左侧列表的服务域（大类）
   * 前端用于渲染左侧的“服务中心”列表
   */
  @GetMapping("/getServiceDomains")
  public RespBean getServiceDomains() {
    List<ServiceDomain> domains = userService.getAllServiceDomains();
    return RespBean.ok(domains);
  }

  @GetMapping("/choseUser")
  public UserGetVO choseUser(Integer id){
    return userService.queryById(id);
  }

  /**
   * 获取最近七天的会话
   * @return
   */
  @GetMapping("/getRecentConversation")
  public RespBean getRecentConversation(){
    log.info("获取最近七天的会话");
    User currentUser = (User) BaseContext.getCurrent();
    List<UserGetVO> recentConversation = userService.getRecentConversation(currentUser.getServiceDomainId(), currentUser.getId());
    return RespBean.ok(recentConversation);
  }


}
