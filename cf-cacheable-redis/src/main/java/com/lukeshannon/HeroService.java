package com.lukeshannon;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class HeroService {
	
	
	private static final Logger log = LoggerFactory.getLogger(HeroService.class);

	
	private Map<String, String> heroes;
	
	@PostConstruct
	public void initHeroes() {
		heroes = new HashMap<String,String>();
		heroes.put("john", "john_blum");
		heroes.put("josh", "starbuxman");
		heroes.put("stu", "svrc");
		heroes.put("dormain", "DormainDrewitz");
		heroes.put("cornelia", "cdavisafc");
		heroes.put("saman", "err_sage");
		heroes.put("casey", "caseywest");
		heroes.put("kenny", "kennybastani");
		heroes.put("jim", "JavaFXpert");
		heroes.put("mark", "MkHeck");
		heroes.put("bridgette", "bridgetkromhout");
	}
	
	@Cacheable(cacheNames = "hero", key="#key.id", unless="#result == null")
	public String expensiveLookUp(RedisKey key) {
		log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
		log.info("$$$$$$$$$$$$$$$ In the expensive look up for the name: " + key.getId() + " $$$$$$$$$$$$$$$");
		log.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
		return heroes.get(key.getId());
	}

}
