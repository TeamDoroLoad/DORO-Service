package com.doroload.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DoroLoadApiApplication {

    // 애플리케이션 진입점
    public static void main(String[] args) {
        SpringApplication.run(DoroLoadApiApplication.class, args);
    }
}
