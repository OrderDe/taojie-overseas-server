package com.zbkj.overseas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Overseas mall application entry.
 * Mirrors tjmall's front application class (com.zbkj.front.TjAppApplication):
 * a single Spring Boot app; the gateway/admin/microservice modules of tjmall
 * are intentionally not carried over — this service is deployed standalone.
 */
@SpringBootApplication
@MapperScan("com.zbkj.overseas.dao")
public class TjOverseasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TjOverseasApplication.class, args);
    }
}
