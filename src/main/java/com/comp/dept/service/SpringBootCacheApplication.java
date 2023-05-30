package com.homedepot.assortment.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class SpringBootCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootCacheApplication.class, args);
    }

}
