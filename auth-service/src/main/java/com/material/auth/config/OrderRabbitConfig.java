package com.material.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderRabbitConfig {
    public static final String ORDER_EXCHANGE = "material.order.exchange";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "material.order.dlx";
    public static final String ORDER_CREATED_QUEUE = "material.order.created.queue";
    public static final String ORDER_CLAIMED_QUEUE = "material.order.claimed.queue";
    public static final String ORDER_CREATED_DEAD_LETTER_QUEUE = "material.order.created.dlq";
    public static final String ORDER_CLAIMED_DEAD_LETTER_QUEUE = "material.order.claimed.dlq";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CLAIMED_ROUTING_KEY = "order.claimed";
    public static final String ORDER_CREATED_DEAD_LETTER_ROUTING_KEY = "order.created.dead";
    public static final String ORDER_CLAIMED_DEAD_LETTER_ROUTING_KEY = "order.claimed.dead";

    /**
     * 作用：声明订单业务交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 DirectExchange，也就是这个方法处理后的结果。
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /**
     * 作用：声明订单死信交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 DirectExchange，也就是这个方法处理后的结果。
     */
    @Bean
    public DirectExchange orderDeadLetterExchange() {
        return new DirectExchange(ORDER_DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * 作用：声明订单创建队列。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Queue，也就是这个方法处理后的结果。
     */
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .deadLetterExchange(ORDER_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CREATED_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 作用：声明订单抢购队列。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Queue，也就是这个方法处理后的结果。
     */
    @Bean
    public Queue orderClaimedQueue() {
        return QueueBuilder.durable(ORDER_CLAIMED_QUEUE)
                .deadLetterExchange(ORDER_DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(ORDER_CLAIMED_DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    /**
     * 作用：声明订单创建死信队列。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Queue，也就是这个方法处理后的结果。
     */
    @Bean
    public Queue orderCreatedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CREATED_DEAD_LETTER_QUEUE).build();
    }

    /**
     * 作用：声明订单抢购死信队列。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Queue，也就是这个方法处理后的结果。
     */
    @Bean
    public Queue orderClaimedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CLAIMED_DEAD_LETTER_QUEUE).build();
    }

    /**
     * 作用：把订单创建队列绑定到订单业务交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Binding，也就是这个方法处理后的结果。
     */
    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderExchange())
                .with(ORDER_CREATED_ROUTING_KEY);
    }

    /**
     * 作用：把订单抢购队列绑定到订单业务交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Binding，也就是这个方法处理后的结果。
     */
    @Bean
    public Binding orderClaimedBinding() {
        return BindingBuilder.bind(orderClaimedQueue())
                .to(orderExchange())
                .with(ORDER_CLAIMED_ROUTING_KEY);
    }

    /**
     * 作用：把订单创建死信队列绑定到死信交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Binding，也就是这个方法处理后的结果。
     */
    @Bean
    public Binding orderCreatedDeadLetterBinding() {
        return BindingBuilder.bind(orderCreatedDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ORDER_CREATED_DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 作用：把订单抢购死信队列绑定到死信交换机。
     * 输入：
     * - 无输入参数。
     * 输出：返回 Binding，也就是这个方法处理后的结果。
     */
    @Bean
    public Binding orderClaimedDeadLetterBinding() {
        return BindingBuilder.bind(orderClaimedDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ORDER_CLAIMED_DEAD_LETTER_ROUTING_KEY);
    }
}
