package top.javahai.chatroom.exception;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.javahai.chatroom.entity.RespBean;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(SQLException.class)
  public RespBean sqlExceptionHandler(SQLException e) {
    // 通过错误码判断完整性约束违反
    if (isIntegrityConstraintViolation(e)) {
      return RespBean.error("该数据与其他数据存在关联，无法删除！");
    } else {
      e.printStackTrace();
      return RespBean.error("数据库异常，操作失败！");
    }
  }

  private boolean isIntegrityConstraintViolation(SQLException e) {
    // 检查错误码或错误信息
    return e.getErrorCode() == 1451 ||  // 外键约束违反
            e.getErrorCode() == 1452 ||  // 外键约束违反
            e.getErrorCode() == 1062 ||  // 重复条目
            e.getMessage().toLowerCase().contains("constraint") ||
            e.getMessage().toLowerCase().contains("duplicate");
  }
}
