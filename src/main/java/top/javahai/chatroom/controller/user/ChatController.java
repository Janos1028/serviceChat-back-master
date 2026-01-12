package top.javahai.chatroom.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.entity.vo.UserGetVO;
import top.javahai.chatroom.service.UserService;

import java.util.List;

/**
 * @author Hai
 * @date 2020/6/16 - 21:32
 */
@RestController
@RequestMapping("/user/chat")
public class ChatController {
  @Autowired
  private UserService userService;

  @GetMapping("/getUsersWithoutCurrentUser")
  public List<UserGetVO> getUsersWithoutCurrentUser(){
    return userService.getUsersWithoutCurrentUser();
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
    User currentUser = (User) BaseContext.getCurrent();
    List<UserGetVO> recentConversation = userService.getRecentConversation(currentUser.getId());
    return RespBean.ok(recentConversation);
  }
}
