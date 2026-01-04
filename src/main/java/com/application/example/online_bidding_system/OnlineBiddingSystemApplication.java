package com.application.example.online_bidding_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OnlineBiddingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineBiddingSystemApplication.class, args);
    }

}
