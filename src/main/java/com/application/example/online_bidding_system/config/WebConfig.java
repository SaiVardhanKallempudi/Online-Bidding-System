package com.application. example.online_bidding_system.config;

import org.springframework.context. annotation.Configuration;
import org.springframework. scheduling.annotation.EnableScheduling;
import org.springframework.web. servlet.config.annotation. CorsRegistry;
import org.springframework.web.servlet.config. annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:4200", "https://bidmart.me",           // add production
                        "https://www.bidmart.me")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}