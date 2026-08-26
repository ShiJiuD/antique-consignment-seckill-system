package com.antique.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 接口文档配置
 *
 * <p>启动后访问：<a href="http://localhost:8080/doc.html">http://localhost:8080/doc.html</a>
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Antique 接口文档")
                        .description("古董交易管理系统 API")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Antique")
                                .email("admin@antique.com")));
    }
}
