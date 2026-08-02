package com.smartcommerce.user.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /**
     * @LoadBalanced lets us call http://auth-service/... using the Eureka
     * service NAME instead of a hardcoded host:port — same idea as the
     * Gateway's lb:// routes, just used here for direct service-to-service
     * calls that bypass the Gateway entirely (internal traffic).
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
