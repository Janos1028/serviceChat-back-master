package top.javahai.chatroom.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.javahai.chatroom.entity.RespBean; // 【新增导入】
import top.javahai.chatroom.utils.AliyunOssUtil;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/user")
public class FileController {

  @Autowired
  AliyunOssUtil aliyunOssUtil;

  // 这个旧接口如果没用到可以不管，或者也改成 RespBean
  @PostMapping("/file")
  public RespBean uploadFile(MultipartFile file) {
    try {
      String url = aliyunOssUtil.upload(file.getInputStream(), "avatar", file.getOriginalFilename());
      return RespBean.ok("上传成功", url);
    } catch (Exception e) {
      e.printStackTrace();
      return RespBean.error("上传失败");
    }
  }

  /**
   * 【核心修改】
   * 1. 返回值改为 RespBean
   * 2. 捕获异常
   */
  @PostMapping("/ossFileUpload")
  public RespBean ossFileUpload(@RequestParam("file") MultipartFile file, @RequestParam("module") String module) {
    try {
      String url = aliyunOssUtil.upload(file.getInputStream(), module, file.getOriginalFilename());
      // 返回统一格式：obj 字段存放 url
      return RespBean.ok("上传成功", url);
    } catch (Exception e) {
      e.printStackTrace();
      return RespBean.error("文件上传失败: " + e.getMessage());
    }
  }

  /**
   * 【新增】专门用于注册时的头像上传（公开接口）
   * 路径：/user/public/uploadAvatar
   * 安全措施：
   * 1. 强制限制文件后缀，只许传图片
   * 2. 强制限制文件大小
   * 3. 强制锁定上传目录为 "avatar"，不允许用户自定义 module
   */
  @PostMapping("/public/uploadAvatar")
  public RespBean uploadAvatar(@RequestParam("file") MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return RespBean.error("文件不能为空");
    }

    // 1. 严格校验文件类型
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !originalFilename.contains(".")) {
      return RespBean.error("文件名非法");
    }

    String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    // 白名单机制
    List<String> allowedSuffixes = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    if (!allowedSuffixes.contains(suffix)) {
      return RespBean.error("仅支持 jpg, jpeg, png, gif 图片格式");
    }

    // 2. 校验大小 (比如限制 3MB)
    if (file.getSize() > 5 * 1024 * 1024) {
      return RespBean.error("头像大小不能超过 5MB");
    }

    try {
      // 3. 核心安全点：直接硬编码 module 为 "avatar"
      // 攻击者无法通过参数修改上传目录
      String url = aliyunOssUtil.upload(file.getInputStream(), "avatar", originalFilename);
      return RespBean.ok("上传成功", url);
    } catch (Exception e) {
      e.printStackTrace();
      return RespBean.error("上传失败: " + e.getMessage());
    }
  }
}
