package com.order.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced // This is included so that we won't get an exception when trying to access a url that has
    //multiple instances. It would go to the pick the instance available in the discovery server at that point.
    // We also changed the WebClient too to WebClient.Builder so that the loadBalance property
    //would be implemented too before building the webclient instance
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder();
    }
}
