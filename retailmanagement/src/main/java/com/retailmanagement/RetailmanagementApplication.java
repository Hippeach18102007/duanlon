package com.retailmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RetailmanagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(RetailmanagementApplication.class, args);
        System.out.println("running...");
    }

}
