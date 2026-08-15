package com.illini.grades;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GradesApplication {
    public static void main(String[] args) {
        SpringApplication.run(GradesApplication.class, args);
    }
}