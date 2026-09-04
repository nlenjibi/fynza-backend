package com.aoms.aomsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AomsBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AomsBackendApplication.class, args);
    }

}
