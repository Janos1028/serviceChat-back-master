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
 * jwt令牌校验的拦截器 (用户端)
 */
@Component
@Slf4j
public class JwtTokenUserInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getUserTokenName());

        //2、校验令牌
        try {
            log.info("jwt校验:{}", token);
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }


            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            User user = new User();
            user.setId(Integer.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString()));
            user.setUsername(claims.get(JwtClaimsConstant.USERNAME).toString());
            user.setNickname(claims.get(JwtClaimsConstant.NICKNAME).toString());
            user.setUserTypeId(Integer.valueOf(claims.get(JwtClaimsConstant.USERTYPEID).toString()));
            BaseContext.setCurrent(user);
            return true;
        } catch (Exception ex) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        BaseContext.removeCurrent();
    }
}
