package top.javahai.chatroom.handler.exception;

/**
 * 业务异常
 */
public class StartConversationFailedException extends BaseException {

    public StartConversationFailedException() {
    }

    public StartConversationFailedException(String msg) {
        super(msg);
    }

}
