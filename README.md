# PCF Caching Example
How to work with In Memory Caches/Clusters in a PCF environment

## Patterns Users
This sample will demonstate using:

1. @Cacheable
2. Spring Data Repositories

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

### Configuring The Application To Use Redis

In the application.properties:

```shell

logging.level.com.lukeshannon.controller=DEBUG
redis-service-name=${vcap.services.rediscloud.myredis.name}

```

In our Config class:

```java

```

In our manifest:

```shell

```


