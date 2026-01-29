package top.javahai.chatroom.handler.exception;

/**
 * 账号不存在异常
 */
public class ConverstaionNotFoundException extends BaseException {

    public ConverstaionNotFoundException() {
    }

    public ConverstaionNotFoundException(String msg) {
        super(msg);
    }

}
