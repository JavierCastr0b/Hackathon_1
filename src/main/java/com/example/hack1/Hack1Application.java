package com.example.hack1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class Hack1Application {

    public static void main(String[] args) {
        SpringApplication.run(Hack1Application.class, args);
    }

}
