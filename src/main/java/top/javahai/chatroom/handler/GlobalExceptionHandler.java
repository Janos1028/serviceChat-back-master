package top.javahai.chatroom.handler;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.handler.exception.BaseException;

import java.sql.SQLException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(RuntimeException.class)
  public RespBean handleBusinessException(RuntimeException e) {
    return RespBean.error(e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public RespBean handleIllegalArgumentException(IllegalArgumentException e) {
    return RespBean.error("参数错误: " + e.getMessage());
  }

  @ExceptionHandler(SQLException.class)
  public RespBean sqlExceptionHandler(SQLException e) {
    if (isIntegrityConstraintViolation(e)) {
      return RespBean.error("该数据与其他数据存在关联，无法删除！");
    } else {
      e.printStackTrace();
      return RespBean.error("数据库异常，操作失败！");
    }
  }

  private boolean isIntegrityConstraintViolation(SQLException e) {
    return e.getErrorCode() == 1451 ||
            e.getErrorCode() == 1452 ||
            e.getErrorCode() == 1062 ||
            e.getMessage().toLowerCase().contains("constraint") ||
            e.getMessage().toLowerCase().contains("duplicate");
  }
}
