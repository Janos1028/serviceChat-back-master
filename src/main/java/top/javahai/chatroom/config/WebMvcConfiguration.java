package top.javahai.chatroom.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
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
    // 【新增】从配置文件中读取允许跨域的前端地址
    @Value("${chatroom.cors.allowed-origins}")
    private String allowedOrigins;
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

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 匹配所有接口路径
                .allowedOrigins(allowedOrigins.split(","))
                // 如果有多前端地址，可以这样写：.allowedOrigins("http://120.55.5.60:90", "http://your-domain.com")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许的请求头
                .exposedHeaders("Verify-Key")
                .allowCredentials(true) // 是否允许发送Cookie等凭证
                .maxAge(3600); // 预检请求的缓存时间(秒)

    }
}
