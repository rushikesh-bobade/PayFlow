package com.payflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class PayflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayflowApplication.class, args);
    }
}
