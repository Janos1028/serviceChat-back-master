package top.javahai.chatroom.controller.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import top.javahai.chatroom.config.VerificationCode;
import top.javahai.chatroom.entity.RespBean;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping
@Slf4j
public class VerifyCodeController {

  @Autowired
  private StringRedisTemplate redisTemplate;
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

    // 生成唯一ID作为key存在redis中，并将验证码作为value存入
    String verifyKey = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set("verify_code:" + verifyKey , text , 60, TimeUnit.SECONDS);
    log.info("生成验证码：key={}, code={}", verifyKey, text);
    // 将verify_key返回给前端，用于后续验证
    response.setHeader("Verify-Key", verifyKey);

    VerificationCode.output(image,response.getOutputStream());
  }

  /**
   * 获取邮箱验证码，并保存到本次会话
   * @param session
   */
  @GetMapping("/mailVerifyCode")
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
