package com.smartcommerce.cart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    /**
     * We store everything as plain strings: key = "cart:{userId}",
     * hash field = productId (as a string), hash value = quantity (as a
     * string). Simple and human-readable if you inspect it with
     * redis-cli — no need for a custom serializer/object mapping for
     * something this simple.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
