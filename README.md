# PCF Caching Example
How to work with In Memory Caches/Clusters in a PCF environment

## Patterns Used
This sample will demonstate using @Cacheable. This standard provides an abstracted way of working with an In-Memory cache. The advantage of this approach is:

1. Allows for the backing cache technology to be changed to different vendors, provided they support the standard
2. Developers are abstracted from the complexity of the in-memory cache

## Caches Used

This sample will show how to work with a Redis service created using the CF Marketplace and bound to the application during a `cf push` operation

### Working With Redis

First step is creating the Redis Cache. To find out what your Redis service is called, run the following:

```shell

cf marketplace

```

There will be an entry like this, if there is not, talk to your Admin about install the tile using Pivotal Cloud Foundry's operations manager (tile can be found here: https://network.pivotal.io/products/redis-labs-enterprise-cluster-for-pcf-service-broker):

```shell

rediscloud  100mb*, 250mb*, 500mb*, 1gb*, 2-5gb*, 5gb*, 10gb*, 50gb*, 30mb   Enterprise-Class Redis for Developers  

```

In the above, the service is called 'rediscloud'. The plans are what follows the service name. Select a plan with caution, there may be charges associated with it.

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

In the pom.xml we give the following dependancies. These are for a Spring Boot Rest API webapplication.

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

Our Config class lets Spring Boot know where the Cache to create the Cache Manager that will be backing the @Cachable Annotation:

```java

@Configuration
public class CacheMangerConfig extends CachingConfigurerSupport {

	  @Bean
	  public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
	    RedisCacheManager cacheManager = RedisCacheManager.create(redisConnectionFactory);
	    return cacheManager;
	  }

}

```

The RedisConnectionFactory that is passed it to create out cacheManager is being created by Spring Cloud Connector dependancies.

In our manifest we bind the redis service to our application:

```shell

---
applications:
- name: cacheable-redis
  memory: 1G
  instances: 1
  path: target/cacheable-redis.jar
  buildpack: https://github.com/cloudfoundry/java-buildpack
  random-route: true
  services:
    - my-redis

```

#### Testing The Cache

When we first hit an endpoint and pass an arguement. We get all the logic that runs in the method, including the Expensive logging statement that is generated by running the body of the arguement. This is because there is no key in the cache for this value.

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

# Warming Up The Cache

PCF has great support for tasks:

https://docs.pivotal.io/pivotalcf/2-0/devguide/using-tasks.html

These are applications that are ran in a container once, then the container is destroyed. However the application remains in your space allowing for logs to be viewed and of course the application to be executed again.

The cf-cache-warm-up application does this by hitting our caching application with specific values to get the value from our backing store and into the cache (thus improving performance).

Here is how the applications is deployed:

```shell

cf push --health-check-type none -p target/cf-cache-warm-up-0.0.1-SNAPSHOT.jar cache-warmer

```
The application will show in the app list as started, but note that there are 0 instances of it.

```shell

name                   requested state   instances   memory   disk   urls
cache-warmer           started           0/1         1G       1G     cache-warmer.cfapps.io
cacheable-redis-luke   started           1/1         1G       1G     cacheable-redis-luke.cfapps.io

```

To run this task:

```shell

 cf-cachable git:(master) cf run-task cache-warmer ".java-buildpack/open_jdk_jre/bin/java org.springframework.boot.loader.JarLauncher --warmUpName=john" --name warm-it-up
Creating task for app cache-warmer in org cloud-native / space cdp-developer-training as luke.shannon@gmail.com...
OK

Task has been submitted successfully for execution.
task name:   warm-it-up
task id:     6

```
We can see based on the id, this is the 6th time the task has been ran. Notice also an arguement is passed in called warmUpName. This is accessed in the Boot application using @Value.

In checking the logs we can see our task was performed before the application shut down.

```shell

cf logs cache-warmer --recent

2018-05-23T15:53:15.53-0400 [CELL/0] OUT Cell cd3a6a7e-8bb3-4631-a839-296095130133 successfully created container for instance c549065a-3a87-40af-9b45-44a18900c3ca
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT   .   ____          _            __ _ _
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT   '  |____| .__|_| |_|_| |_\__, | / / / /
   2018-05-23T15:53:22.11-0400 [APP/TASK/warm-it-up/0] OUT  =========|_|==============|___/=/_/_/_/
   2018-05-23T15:53:22.12-0400 [APP/TASK/warm-it-up/0] OUT  :: Spring Boot ::        (v2.0.2.RELEASE)
   2018-05-23T15:53:22.35-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:22.350  INFO 7 --- [           main] pertySourceApplicationContextInitializer : 'cloud' property source added
   2018-05-23T15:53:22.36-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:22.362  INFO 7 --- [           main] nfigurationApplicationContextInitializer : Reconfiguration enabled
   2018-05-23T15:53:22.39-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:22.384  INFO 7 --- [           main] c.lukeshannon.CfCacheWarmUpApplication   : Starting CfCacheWarmUpApplication on c549065a-3a87-40af-9b45-44a18900c3ca with PID 7 (/home/vcap/app/BOOT-INF/classes started by vcap in /home/vcap/app)
   2018-05-23T15:53:22.39-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:22.395  INFO 7 --- [           main] c.lukeshannon.CfCacheWarmUpApplication   : The following profiles are active: cloud
   2018-05-23T15:53:22.53-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:22.532  INFO 7 --- [           main] s.c.a.AnnotationConfigApplicationContext : Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@43556938: startup date [Wed May 23 19:53:22 UTC 2018]; root of context hierarchy
   2018-05-23T15:53:24.37-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:24.375  INFO 7 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
   2018-05-23T15:53:24.38-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:24.387  INFO 7 --- [           main] o.s.c.support.DefaultLifecycleProcessor  : Starting beans in phase 0
   2018-05-23T15:53:24.41-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:24.410  INFO 7 --- [           main] c.lukeshannon.CfCacheWarmUpApplication   : Started CfCacheWarmUpApplication in 2.803 seconds (JVM running for 4.227)
   2018-05-23T15:53:24.78-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:24.786  INFO 7 --- [           main] c.lukeshannon.CfCacheWarmUpApplication   : Warming up cache for value john
   2018-05-23T15:53:25.30-0400 [APP/TASK/warm-it-up/0] OUT HTTP/1.1 200 OK
   2018-05-23T15:53:25.31-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:25.316  INFO 7 --- [       Thread-2] s.c.a.AnnotationConfigApplicationContext : Closing org.springframework.context.annotation.AnnotationConfigApplicationContext@43556938: startup date [Wed May 23 19:53:22 UTC 2018]; root of context hierarchy
   2018-05-23T15:53:25.31-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:25.319  INFO 7 --- [       Thread-2] o.s.c.support.DefaultLifecycleProcessor  : Stopping beans in phase 0
   2018-05-23T15:53:25.32-0400 [APP/TASK/warm-it-up/0] OUT 2018-05-23 19:53:25.321  INFO 7 --- [       Thread-2] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
   2018-05-23T15:53:25.35-0400 [APP/TASK/warm-it-up/0] OUT Exit status 0
   2018-05-23T15:53:25.36-0400 [CELL/0] OUT Cell cd3a6a7e-8bb3-4631-a839-296095130133 stopping instance c549065a-3a87-40af-9b45-44a18900c3ca
   2018-05-23T15:53:25.36-0400 [CELL/0] OUT Cell cd3a6a7e-8bb3-4631-a839-296095130133 destroying container for instance c549065a-3a87-40af-9b45-44a18900c3ca
   2018-05-23T15:53:25.77-0400 [CELL/0] OUT Cell cd3a6a7e-8bb3-4631-a839-296095130133 successfully destroyed container for instance c549065a-3a87-40af-9b45-44a18900c3ca

```

Note: Logs information from all 6 invocations of the task will be in this log.

