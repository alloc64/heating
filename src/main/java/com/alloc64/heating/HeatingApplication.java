package com.alloc64.heating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication(scanBasePackages = "com.alloc64.*")
@EntityScan("com.alloc64.*")
public class HeatingApplication {
    public static void main(String[] args) {
        SpringApplication.run(HeatingApplication.class, args);
    }
}
