package com.lukeshannon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableCaching
public class CfCacheableRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfCacheableRedisApplication.class, args);
	}
	
	@RestController
	class HomeController {
		@GetMapping("/")
		public String home() {
			return "Run /v1/getTwitterHandle/{name} to find the twitter handle of your hero";
		}
	}
	
}
