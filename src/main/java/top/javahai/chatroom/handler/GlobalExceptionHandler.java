package top.javahai.chatroom.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.javahai.chatroom.entity.RespBean;
import top.javahai.chatroom.handler.exception.BaseException;

import java.sql.SQLException;

/**
 * 全局异常处理器
 * 作用：拦截 Controller 层抛出的各种异常，统一处理后返回标准 JSON 格式给前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 添加日志记录器，用于记录未知的系统异常
  private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * 【核心修改】处理自定义业务异常
   * 场景：业务逻辑校验失败（如：LoginFailedException, StartConversationFailedException 等）
   * 处理：直接返回异常中的 message 给用户看
   */
  @ExceptionHandler(BaseException.class)
  public RespBean handleBaseException(BaseException e) {
    return RespBean.error(e.getMessage());
  }

  /**
   * 【核心修改】处理参数非法异常
   * 场景：前端传参不对
   * 处理：可以适当提示参数错误
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public RespBean handleIllegalArgumentException(IllegalArgumentException e) {
    logger.warn("参数异常: {}", e.getMessage());
    return RespBean.error("请求参数错误: " + e.getMessage());
  }

  /**
   * 场景：空指针 (NullPointerException)、数组越界、类型转换错误等 Bug
   * 处理：
   * 1. 绝对不能把 e.getMessage() 给用户看（不友好且不安全）
   * 2. 必须记录错误日志，方便开发人员排查
   * 3. 给用户返回通用的“系统繁忙”提示
   */
  @ExceptionHandler(RuntimeException.class)
  public RespBean handleRuntimeException(RuntimeException e) {
    // 1. 记录详细堆栈日志 (非常重要！)
    logger.error("系统发生未知异常:", e);

    // 2. 告诉前端一个模糊的提示
    return RespBean.error("系统繁忙，请稍后再试！");
  }

  /**
   * 处理数据库异常
   */
  @ExceptionHandler(SQLException.class)
  public RespBean sqlExceptionHandler(SQLException e) {
    if (isIntegrityConstraintViolation(e)) {
      return RespBean.error("该数据与其他数据存在关联，无法删除！");
    } else {
      // 记录数据库异常日志
      logger.error("数据库操作异常:", e);
      return RespBean.error("数据库异常，操作失败！");
    }
  }

  /**
   * 判断是否是外键约束或唯一约束违反
   */
  private boolean isIntegrityConstraintViolation(SQLException e) {
    return e.getErrorCode() == 1451 ||
            e.getErrorCode() == 1452 ||
            e.getErrorCode() == 1062 ||
            (e.getMessage() != null && (
                    e.getMessage().toLowerCase().contains("constraint") ||
                            e.getMessage().toLowerCase().contains("duplicate")
            ));
  }
}
