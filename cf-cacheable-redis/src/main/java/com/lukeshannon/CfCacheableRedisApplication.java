package com.lukeshannon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CfCacheableRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfCacheableRedisApplication.class, args);
	}
}
