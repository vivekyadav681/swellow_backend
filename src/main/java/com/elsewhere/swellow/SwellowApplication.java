package com.elsewhere.swellow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SwellowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwellowApplication.class, args);
    }
}
