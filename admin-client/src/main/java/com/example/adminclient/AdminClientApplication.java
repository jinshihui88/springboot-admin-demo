package com.example.adminclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Admin Client：被监控的业务应用。
 *
 * 引入 spring-boot-admin-starter-client 后，
 * 按 spring.boot.admin.client.url 配置的地址周期性注册。
 */
@SpringBootApplication
public class AdminClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminClientApplication.class, args);
    }
}
