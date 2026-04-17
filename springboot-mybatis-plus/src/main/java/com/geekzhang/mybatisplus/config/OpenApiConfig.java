package com.geekzhang.mybatisplus.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI配置类 (替代Springfox)
 *
 * @author geekzhang
 * @since 2026-02-26
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringBoot + MyBatis-Plus 接口文档")
                        .description("基于SpringBoot和MyBatis-Plus的RESTful API接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("geekzhang")
                                .url("https://github.com/geekzhang")
                                .email("geekzhang@example.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
