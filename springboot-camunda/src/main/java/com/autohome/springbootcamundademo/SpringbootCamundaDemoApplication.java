package com.autohome.springbootcamundademo;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableProcessApplication
@EnableScheduling
@MapperScan("com.autohome.springbootcamundademo.mybatis.mapper.**")
public class SpringbootCamundaDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootCamundaDemoApplication.class, args);
    }

}
