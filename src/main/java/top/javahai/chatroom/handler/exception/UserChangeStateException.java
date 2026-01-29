package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class UserChangeStateException extends BaseException {

    public UserChangeStateException() {
    }

    public UserChangeStateException(String msg) {
        super(msg);
    }

}
