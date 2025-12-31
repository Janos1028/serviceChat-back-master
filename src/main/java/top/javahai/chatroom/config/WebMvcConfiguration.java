package top.javahai.chatroom.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.javahai.chatroom.interceptor.JwtTokenAdminInterceptor;
import top.javahai.chatroom.interceptor.JwtTokenUserInterceptor;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;

    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册管理端拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");

        // 注册用户端拦截器
        // 假设用户端接口以 /user 或其他开头，根据实际 Controller 调整
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**")
                .excludePathPatterns(
                        "/user/login",
                        "/user/register",
                        "/doLogin",
                        "/login",
                        "/user/checkUsername",
                        "/user/checkNickname",
                        "/user/public/uploadAvatar"
                );
    }
}
