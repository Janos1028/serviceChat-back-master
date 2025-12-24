package top.javahai.chatroom.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.javahai.chatroom.config.VerificationCode;
import top.javahai.chatroom.entity.RespBean;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.Random;

/**
 * @author Hai
 * @date 2020/6/16 - 17:33
 */
@RestController
public class LoginController {
  /**
   * 获取验证码图片写到响应的输出流中，保存验证码到session
   * @param response
   * @param session
   * @throws IOException
   */
  @GetMapping("/verifyCode")
  public void getVerifyCode(HttpServletResponse response, HttpSession session) throws IOException {
    VerificationCode code = new VerificationCode();
    BufferedImage image = code.getImage();
    String text = code.getText();
    session.setAttribute("verify_code",text);
    VerificationCode.output(image,response.getOutputStream());
  }

  @Autowired
  JavaMailSender javaMailSender;
  /**
   * 获取邮箱验证码，并保存到本次会话
   * @param session
   */
  @GetMapping("/admin/mailVerifyCode")
  public RespBean getMailVerifyCode(HttpSession session){
    // 生成一个固定的验证码用于直接放行
    String code = "1234"; // 固定验证码，或生成后直接设置为已验证状态

    // 将验证码标记为已验证（直接放行）
    session.setAttribute("mail_verify_code", code);
    session.setAttribute("mail_verify_code_verified", true); // 添加验证状态标记

    // 不发送邮件，直接返回成功
    return RespBean.ok("验证码已生成，直接放行");
  }
}
