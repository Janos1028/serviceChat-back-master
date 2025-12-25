package top.javahai.chatroom.controller;

import org.csource.common.MyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.javahai.chatroom.utils.FastDFSUtil;
import top.javahai.chatroom.utils.AliyunOssUtil;

import java.io.IOException;

/**
 * @author Hai
 * @date 2020/6/21 - 16:47
 */
@RestController
@RequestMapping("/user")
public class FileController {


  @Autowired
  AliyunOssUtil aliyunOssUtil;

  @PostMapping("/file")
  public String uploadFile(MultipartFile file) throws IOException, MyException {
    // 使用阿里云 OSS 上传，module 可以设为默认值如 "avatar"
    return aliyunOssUtil.upload(file.getInputStream(), "avatar", file.getOriginalFilename());
  }

  @PostMapping("/ossFileUpload")
  public String ossFileUpload(@RequestParam("file")MultipartFile file,@RequestParam("module") String module) throws IOException, MyException {
    return aliyunOssUtil.upload(file.getInputStream(),module,file.getOriginalFilename());
  }

}
