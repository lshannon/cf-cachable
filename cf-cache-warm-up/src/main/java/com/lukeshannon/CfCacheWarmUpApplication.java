package com.lukeshannon;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.context.annotation.Bean;

@EnableTask
@SpringBootApplication
public class CfCacheWarmUpApplication {
	
	private static final Logger log = LoggerFactory.getLogger(CfCacheWarmUpApplication.class);


	@Bean
	public CommandLineRunner commandLineRunner() {
		return new CacheWarmerCommandLineRunner();
	}

	
	public static class CacheWarmerCommandLineRunner implements CommandLineRunner {
		
		@Value("${warmUpName:josh}")
		private String warmUpName;

		@Override
		public void run(String... strings) throws Exception {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet("https://cacheable-redis-luke.cfapps.io/v1/getTwitterHandle/" + warmUpName);
			log.info("Warming up cache for value " + warmUpName);
			CloseableHttpResponse response1 = httpclient.execute(httpGet);
			try {
			    System.out.println(response1.getStatusLine());
			    HttpEntity entity1 = response1.getEntity();
			    EntityUtils.consume(entity1);
			} finally {
				response1.close();
			}
		}
	}
	
	public static void main(String[] args) {
		SpringApplication.run(CfCacheWarmUpApplication.class, args);
	}
}
