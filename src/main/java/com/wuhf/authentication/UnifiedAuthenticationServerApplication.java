package com.wuhf.authentication;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wuhf.authentication.dao")
public class UnifiedAuthenticationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedAuthenticationServerApplication.class, args);
    }

}
