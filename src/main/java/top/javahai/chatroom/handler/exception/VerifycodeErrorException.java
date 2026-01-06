package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class VerifycodeErrorException extends BaseException {

    public VerifycodeErrorException() {
    }

    public VerifycodeErrorException(String msg) {
        super(msg);
    }

}
