package top.javahai.chatroom.config;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String DELAY_EXCHANGE_NAME = "delay_exchange";
    public static final String PROCESS_EXCHANGE_NAME = "process_exchange";

    public static final String QUEUE_5M = "queue_5m";
    public static final String QUEUE_30M = "queue_30m";
    public static final String PROCESS_QUEUE = "process_queue";

    public static final String ROUNTING_KEY_5M = "task.5m";
    public static final String ROUNTING_KEY_30M = "task.30m";
    public static final String PROCESS_ROUTING_KEY = "task.process";

    // 1. 业务处理交换机和队列
    @Bean
    public DirectExchange processExchange() { return new DirectExchange(PROCESS_EXCHANGE_NAME); }
    @Bean
    public Queue processQueue() { return new Queue(PROCESS_QUEUE); }
    @Bean
    public Binding processBinding() {
        return BindingBuilder.bind(processQueue()).to(processExchange()).with(PROCESS_ROUTING_KEY);
    }

    // 2. 延时(死信)交换机
    @Bean
    public DirectExchange delayExchange() { return new DirectExchange(DELAY_EXCHANGE_NAME); }

    // 3. 5分钟延时队列
    @Bean
    public Queue queue5m() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PROCESS_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", PROCESS_ROUTING_KEY);
        args.put("x-message-ttl", 5 * 60 * 1000); // 5分钟
        return QueueBuilder.durable(QUEUE_5M).withArguments(args).build();
    }

    // 4. 30分钟延时队列
    @Bean
    public Queue queue30m() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", PROCESS_EXCHANGE_NAME);
        args.put("x-dead-letter-routing-key", PROCESS_ROUTING_KEY);
        args.put("x-message-ttl", 30 * 60 * 1000); // 30分钟
        return QueueBuilder.durable(QUEUE_30M).withArguments(args).build();
    }

    @Bean
    public Binding binding5m() {
        return BindingBuilder.bind(queue5m()).to(delayExchange()).with(ROUNTING_KEY_5M);
    }
    @Bean
    public Binding binding30m() {
        return BindingBuilder.bind(queue30m()).to(delayExchange()).with(ROUNTING_KEY_30M);
    }
}
