package com.biggerboy.springaidemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author BiggerBoy
 * @date 2025/5/7
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/ai/**")
                .allowedOrigins("http://localhost:5173") // 你的前端地址
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
        registry.addMapping("/conversations/**")
                .allowedOrigins("http://localhost:5173") // 你的前端地址
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
        registry.addMapping("/conversationMessage/**")
                .allowedOrigins("http://localhost:5173") // 你的前端地址
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
