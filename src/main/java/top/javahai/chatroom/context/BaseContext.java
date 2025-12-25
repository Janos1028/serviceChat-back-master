package top.javahai.chatroom.context;


/**
 * 线程上下文工具类，用于存储当前请求的用户ID
 */
public class BaseContext {

    public static ThreadLocal<Object> threadLocal = new ThreadLocal<>();

    public static void setCurrent(Object object) {
        threadLocal.set(object);
    }

    public static Object getCurrent() {
        return threadLocal.get();
    }

    public static void removeCurrent() {
        threadLocal.remove();
    }
}
