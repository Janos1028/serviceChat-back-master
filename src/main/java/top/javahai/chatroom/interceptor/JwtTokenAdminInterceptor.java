package top.javahai.chatroom.interceptor;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import top.javahai.chatroom.config.JwtProperties;
import top.javahai.chatroom.constant.JwtClaimsConstant;
import top.javahai.chatroom.context.BaseContext;
import top.javahai.chatroom.entity.User;
import top.javahai.chatroom.utils.JwtUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * jwt令牌校验的拦截器 (管理端)
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 校验jwt
     */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是Controller的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getAdminTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            // 兼容 Bearer 开头的 token
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            User user = new User();
            user.setId(Integer.valueOf(claims.get(JwtClaimsConstant.ADMIN_ID).toString()));
            user.setUsername(claims.get(JwtClaimsConstant.USERNAME).toString());
            user.setNickname(claims.get(JwtClaimsConstant.NICKNAME).toString());
            user.setServiceDomainId(Integer.valueOf(claims.get(JwtClaimsConstant.SERVICE_DOMAIN_ID).toString()));
            // 存入 ThreadLocal
            BaseContext.setCurrent(user);
            return true;
        } catch (Exception ex) {
            //4、不通过，响应 401 状态码
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清理 ThreadLocal，防止内存泄漏
        BaseContext.removeCurrent();
    }
}
