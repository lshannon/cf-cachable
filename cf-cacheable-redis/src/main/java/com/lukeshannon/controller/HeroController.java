/**
 * 
 */
package com.lukeshannon.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lukeshannon.HeroService;
import com.lukeshannon.RedisKey;

/**
 * @author lshannon
 *
 */
@RestController
@RequestMapping("/v1")
public class HeroController {
	
	private static final Logger log = LoggerFactory.getLogger(HeroController.class);
	
	private HeroService heroService;
	
	public HeroController(HeroService heroService) {
		this.heroService = heroService;
	}

	@GetMapping("/getTwitterHandle/{name}")
	public String getHero(@PathVariable String name) {
		log.info("Making a call to the Hero service");
		RedisKey key = new RedisKey();
		key.setId(name);
		key.setName(name);
		String result = heroService.expensiveLookUp(key);
		log.info("Returning result: " + result);
		if (result == null) {
			return "No result for: " + name;
		}
		return result;
	}


	
	
}

