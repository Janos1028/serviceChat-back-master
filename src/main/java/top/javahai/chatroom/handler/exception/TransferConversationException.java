package top.javahai.chatroom.handler.exception;

/**
 * 账号被锁定异常
 */
public class TransferConversationException extends BaseException {

    public TransferConversationException() {
    }

    public TransferConversationException(String msg) {
        super(msg);
    }

}
