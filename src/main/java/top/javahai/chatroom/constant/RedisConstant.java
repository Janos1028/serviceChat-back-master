package top.javahai.chatroom.constant;

public class RedisConstant {
    public static final String SESSION_KEY_PREFIX = "chat:active_sessions:";
    public static final String VERIFY_CODE_KEY = "verify_code:";
    public static final String USER_ONLINE = "user_online:";
    public static final String RATE_KEY_PREFIX = "conversation:rate:timeout:";
    public static final String FIRST_RESPONSE_TIMEOUT_KEY = "conversation:first_response:timeout:";
    public static final String ALL_SERVICE_DOMAINS_KEY = "all_service_domains";
    public static final String AUTO_CLOSE_KEY_PREFIX = "auto_close:";
}
