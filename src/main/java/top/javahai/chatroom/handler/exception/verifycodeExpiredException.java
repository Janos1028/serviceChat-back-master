package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class verifycodeExpiredException extends BaseException {

    public verifycodeExpiredException() {
    }

    public verifycodeExpiredException(String msg) {
        super(msg);
    }

}
