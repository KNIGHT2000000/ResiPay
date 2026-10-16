package com.resipay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResiPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResiPayApplication.class, args);
    }
}

