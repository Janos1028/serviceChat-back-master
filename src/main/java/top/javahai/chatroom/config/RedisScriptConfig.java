package top.javahai.chatroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisScriptConfig {

    @Bean
    public DefaultRedisScript<Long> unlockRedisScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        // 指定 Lua 脚本在 resources 目录下的相对路径
        redisScript.setLocation(new ClassPathResource("unlock.lua"));
        // 指定 Lua 脚本执行后的返回值类型
        redisScript.setResultType(Long.class);
        return redisScript;
    }
}
