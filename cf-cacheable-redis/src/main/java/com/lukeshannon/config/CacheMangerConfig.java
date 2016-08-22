/**
 * 
 */
package com.lukeshannon.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @author lshannon
 *
 */
public class CacheMangerConfig extends CachingConfigurerSupport {
	
	@Value("${redis-service-name}")
	private String redisServiceName;
	
	@Bean
	  public RedisConnectionFactory redisConnectionFactory() {
		CloudFactory cloudFactory = new CloudFactory();
        Cloud cloud = cloudFactory.getCloud();
        RedisServiceInfo serviceInfo = (RedisServiceInfo) cloud.getServiceInfo(redisServiceName);
        String serviceID = serviceInfo.getId();
        return cloud.getServiceConnector(serviceID, RedisConnectionFactory.class, null);
	  }

	  @Bean
	  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
	    RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
	    redisTemplate.setConnectionFactory(cf);
	    return redisTemplate;
	  }

	  @Bean
	  public CacheManager cacheManager(RedisTemplate redisTemplate) {
	    RedisCacheManager cacheManager = new RedisCacheManager(redisTemplate);
	    cacheManager.setDefaultExpiration(300);
	    return cacheManager;
	  }

}
