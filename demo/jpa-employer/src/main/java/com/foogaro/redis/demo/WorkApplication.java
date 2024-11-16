package com.foogaro.redis.demo;

import com.foogaro.redis.wbs.core.config.WBSConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRedisRepositories(basePackages = "com.foogaro.redis.demo.repository.redis")
@EnableJpaRepositories(basePackages = "com.foogaro.redis.demo.repository.jpa")
@EnableScheduling
@Import(WBSConfiguration.class)  // Add this
public class WorkApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkApplication.class, args);
	}

}
