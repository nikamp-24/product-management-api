package com.productmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProductManagementApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductManagementApiApplication.class, args);
    }

}
