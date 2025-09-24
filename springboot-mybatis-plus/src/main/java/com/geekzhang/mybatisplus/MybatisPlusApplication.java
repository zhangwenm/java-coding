package com.geekzhang.mybatisplus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * SpringBoot + MyBatis-Plus 主启动类
 *
 * @author geekzhang
 * @since 2025-09-24
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.geekzhang.mybatisplus.mapper")
public class MybatisPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MybatisPlusApplication.class, args);
        System.out.println("=================================");
        System.out.println("SpringBoot + MyBatis-Plus 应用启动成功!");
        System.out.println("Swagger文档地址: http://localhost:8080/swagger-ui/");
        System.out.println("Druid监控地址: http://localhost:8080/druid/");
        System.out.println("=================================");
    }
}
