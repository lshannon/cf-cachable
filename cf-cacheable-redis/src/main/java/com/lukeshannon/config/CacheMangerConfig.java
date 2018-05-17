/**
 * 
 */
package com.lukeshannon.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * @author lshannon
 *
 */
@Configuration
public class CacheMangerConfig extends CachingConfigurerSupport {

	  @Bean
	  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
	    RedisCacheManager cacheManager = RedisCacheManager.create(redisConnectionFactory);
	    return cacheManager;
	  }

}
