package com.biggerboy.springaidemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author 公众号：BiggerBoy
 * @date 2025/5/7
 */
@SpringBootApplication
@MapperScan("com.biggerboy.springaidemo.mapper")
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }

}
