package com.dyouwang.xiangqi.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 启动入口
 */
@SpringBootApplication // 这是一个 Spring Boot 应用
public class ServerApplication {

    public static void main(String[] args) {
        // 启动 Spring Boot 应用
        SpringApplication.run(ServerApplication.class, args);
        System.out.println("Xiangqi Server is running!");
    }
}