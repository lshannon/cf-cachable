# PCF Caching Example
How to work with In Memory Caches/Clusters in a PCF environment

## Patterns Used
This sample will demonstate using @Cacheable. This is the standard that gives an abstracted way of working with an In-Memory cache. The advantage of this approach is it:

1. Allows for the backing cache technology to be changed to different vendors, provided they support the standard
2. Developers are abstracted from the complexity of the in-memory cache

## Caches Used

This sample will show how to work with a Redis and Gemfire Cache

### Working With Redis

First step is creating the Redis Cache. To find out what your Redis service is called, run the following:

```shell

cf marketplace

```

There will be an entry like this, if there is not, talk to your Admin about install the tile using Pivotal Cloud Foundry's operations manager (tile can be found here: https://network.pivotal.io/products/redis-labs-enterprise-cluster-for-pcf-service-broker):

```shell

rediscloud  100mb*, 250mb*, 500mb*, 1gb*, 2-5gb*, 5gb*, 10gb*, 50gb*, 30mb   Enterprise-Class Redis for Developers  

```

This means the service is called 'rediscloud'. The plans are what follows the service name. Select a plan with caution, there may be a charge associated with it.

Here is how we create a service plan in our space.

```shell

cf create-service rediscloud 30mb my-redis
Creating service instance my-redis in org cloud-native / space development as luke.shannon@gmail.com...
OK

```
The cache is now available in the Space for our caching needs.

We can get our details about our Cache by using the cf service command.

```shell

 cf-cachable git:(master) cf service my-redis

Service instance: my-redis
Service: rediscloud
Plan: 30mb
Description: Enterprise-Class Redis for Developers
Documentation url: http://docs.run.pivotal.io/marketplace/services/rediscloud.html
Dashboard: https://cloudfoundry.appdirect.com/api/custom/cloudfoundry/v2/sso/start?serviceUuid=aa34b274-071a-477d-9169-143af9507d8a

Last Operation
Status: create succeeded
Message: 
Started: 2016-08-21T23:09:37Z
Updated: 

```

#### Configuring The Application To Use Redis

In the pom.xml we give the following dependancies. These are for a Spring Boot Rest API webapplication. This was developed for Spring Boot 1.3.7.RELEASE.

```xml

<dependencies>
	
		<!-- Starting Caching Dependancies -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-cache</artifactId>
		</dependency>
		<dependency>
			<groupId>redis.clients</groupId>
			<artifactId>jedis</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
			<version>1.4.0.RELEASE</version>
		</dependency>
		<!-- End Caching Dependancies -->
		
		<!-- Starting PCF Connector Dependancies -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-spring-service-connector</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-cloudfoundry-connector</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-localconfig-connector</artifactId>
		</dependency>
		<!-- End PCF Connector Dependancies -->
		
		<!-- Start General Web and Testing Dependancies -->
		<dependency>
    		<groupId>org.springframework.boot</groupId>
    		<artifactId>spring-boot-starter-actuator</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<!-- End General Web and Testing Dependancies -->
		
	</dependencies>


```

In the main method of our Spring Boot application we set the @EnableCaching annotion
```java

@SpringBootApplication
@EnableCaching
public class CfCacheableRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(CfCacheableRedisApplication.class, args);
	}
}

```

Our Config class lets Spring Boot know where the Cache can be found, the `redis-service-name` is set in the application config file:

```java

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

```
In the application.properties we dynamically have our value set to the name of the Redis service.

```shell

logging.level.com.lukeshannon.controller=DEBUG
redis-service-name=${vcap.services.rediscloud.myredis.name}

```

In our manifest we bind the service to our application.

```shell

---
applications:
- name: cacheable-redis
  memory: 1G
  instances: 1
  path: target/cacheable-redis.jar
  buildpack: https://github.com/cloudfoundry/java-buildpack
  services:
    - my-redis

```

#### Testing The Cache

When we first hit an endpoint and pass an arguement. We get all the logic that runs in the method including the Expensive logging statement that is generated by running the body of the arguement. This is because there is no key in the cache for this value.

```shell

cf-cacheable-redis git:(master) ✗ curl http://cacheable-redis.cfapps.io/v1/getTwitterHandle/kenny
kennybastani  

```
Resulting log (note the expensive look up call):
```shell

2016-08-21T22:53:08.45-0400 [APP/0]      OUT 2016-08-22 02:53:08.453 DEBUG 13 --- [nio-8080-exec-5] o.s.b.c.web.OrderedRequestContextFilter  : Cleared thread-bound request context: org.apache.catalina.connector.RequestFacade@74568c3d
2016-08-21T22:53:57.61-0400 [APP/0]      OUT 2016-08-22 02:53:57.616 DEBUG 13 --- [nio-8080-exec-7] o.s.b.c.web.OrderedRequestContextFilter  : Bound request context to thread: org.apache.catalina.connector.RequestFacade@74568c3d
2016-08-21T22:53:57.61-0400 [APP/0]      OUT 2016-08-22 02:53:57.618 DEBUG 13 --- [nio-8080-exec-7] o.s.b.a.e.mvc.EndpointHandlerMapping     : Looking up handler method for path /v1/getTwitterHandle/kenny
2016-08-21T22:53:57.62-0400 [APP/0]      OUT 2016-08-22 02:53:57.620 DEBUG 13 --- [nio-8080-exec-7] o.s.b.a.e.mvc.EndpointHandlerMapping     : Did not find handler method for [/v1/getTwitterHandle/kenny]
2016-08-21T22:53:57.62-0400 [APP/0]      OUT 2016-08-22 02:53:57.624 DEBUG 13 --- [nio-8080-exec-7] c.lukeshannon.controller.HeroController  : $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
2016-08-21T22:53:57.62-0400 [APP/0]      OUT 2016-08-22 02:53:57.624 DEBUG 13 --- [nio-8080-exec-7] c.lukeshannon.controller.HeroController  : $$$$$$$$$$$$$$$ In the expensive look up for the name: kenny $$$$$$$$$$$$$$$
2016-08-21T22:53:57.62-0400 [APP/0]      OUT 2016-08-22 02:53:57.624 DEBUG 13 --- [nio-8080-exec-7] c.lukeshannon.controller.HeroController  : $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
2016-08-21T22:53:57.63-0400 [APP/0]      OUT 2016-08-22 02:53:57.630 DEBUG 13 --- [nio-8080-exec-7] o.s.b.c.web.OrderedRequestContextFilter  : Cleared thread-bound request context: org.apache.catalina.connector.RequestFacade@74568c3d
2016-08-21T22:53:57.64-0400 [RTR/5]      OUT cacheable-redis.cfapps.io - [22/08/2016:02:53:57.627 +0000] "GET /v1/getTwitterHandle/kenny HTTP/1.1" 200 0 12 "-" "curl/7.43.0" 10.10.66.125:49988 x_forwarded_for:"184.151.190.137" x_forwarded_proto:"http" vcap_request_id:8e205519-74a1-46f0-76bb-c92bd88cafd6 response_time:0.01670273 app_id:74e94e7c-9212-4d44-b399-ae245056d67e
```

However, when we hit it again:

```shell

cf-cacheable-redis git:(master) ✗ curl http://cacheable-redis.cfapps.io/v1/getTwitterHandle/kenny
kennybastani  

```

This time we get the same logging however this time without the logging for the method, this is because the value is obtained from Redis:

```shell
o.s.b.c.web.OrderedRequestContextFilter  : Bound request context to thread: org.apache.catalina.connector.RequestFacade@74568c3d
2016-08-21T22:54:08.19-0400 [APP/0]      OUT 2016-08-22 02:54:08.190 DEBUG 13 --- [nio-8080-exec-9] o.s.b.a.e.mvc.EndpointHandlerMapping     : Looking up handler method for path /v1/getTwitterHandle/kenny
2016-08-21T22:54:08.19-0400 [APP/0]      OUT 2016-08-22 02:54:08.192 DEBUG 13 --- [nio-8080-exec-9] o.s.b.a.e.mvc.EndpointHandlerMapping     : Did not find handler method for [/v1/getTwitterHandle/kenny]
2016-08-21T22:54:08.20-0400 [APP/0]      OUT 2016-08-22 02:54:08.201 DEBUG 13 --- [nio-8080-exec-9] o.s.b.c.web.OrderedRequestContextFilter  : Cleared thread-bound request context: org.apache.catalina.connector.RequestFacade@74568c3d
2016-08-21T22:54:08.21-0400 [RTR/5]      OUT cacheable-redis.cfapps.io - [22/08/2016:02:54:08.199 +0000] "GET /v1/getTwitterHandle/kenny HTTP/1.1" 200 0 12 "-" "curl/7.43.0" 10.10.66.125:50014 x_forwarded_for:"184.151.190.137" x_forwarded_proto:"http" vcap_request_id:6e551031-05ae-43b4-758f-7a0d73511b3a response_time:0.016591925 app_id:74e94e7c-9212-4d44-b399-ae245056d67e

```

### Working With Gemfire

The only thing that needs to change in the previous example is how the CacheManger is obtained in the configuration class. Note in the following Configuration for Gemfire:

1. A Client Cache must friend be obtained using the name of the service created on PCF
2. The client cache is wrapped in a Cache Manager
3. A Region needs to be locally created to contain the entries that will be cached. In this case we set the local Region to be a PROXY (meaning it goes to the main cache for each look up). However this can also be a CACHING PROXY, meaning we can have a local cache in our application that is fully configurable (this is a near cache pattern).

```java

@Configuration
@Profile({ "cloud" })
public class CacheConfiguration extends AbstractCloudConfig {

	@Value("${gemfire-service-name}")
	private String gemfireServiceName;
	
	private ServiceConnectorConfig createGemfireConnectorConfig() {
		GemfireServiceConnectorConfig gemfireConfig = new GemfireServiceConnectorConfig();
		return gemfireConfig;
	}


	@Bean
	ClientCache clientCache() {
		CloudFactory cloudFactory = new CloudFactory();
		Cloud cloud = cloudFactory.getCloud();
	    	ClientCache cache = cloud.getServiceConnector(gemfireServiceName, ClientCache.class, createGemfireConnectorConfig());
		return cache;
	}
	
	@Bean
	 public CacheManager cacheManager(final Cache gemfireCache) {
		 return new GemfireCacheManager() {{
	            setCache(gemfireCache);
	        }};
	 }


	@Bean
	public ClientRegionFactoryBean<String, String> heroRegion(ClientCache cache) {
		ClientRegionFactoryBean<String,String> region = new ClientRegionFactoryBean<>();
		region.setCache(cache);
		region.setName("hero");
		region.setShortcut(ClientRegionShortcut.PROXY);
		return region;
	}

}

```

For questions with this sample please contact luke.shannon at gmail dot com.


