package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class VerifycodeEmptyException extends BaseException {

    public VerifycodeEmptyException() {
    }

    public VerifycodeEmptyException(String msg) {
        super(msg);
    }

}
