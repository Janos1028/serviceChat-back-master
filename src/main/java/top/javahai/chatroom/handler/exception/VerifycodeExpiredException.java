package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class VerifycodeExpiredException extends BaseException {

    public VerifycodeExpiredException() {
    }

    public VerifycodeExpiredException(String msg) {
        super(msg);
    }

}
