package com.clouddocs.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableRetry
public class ClouddocsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClouddocsBackendApplication.class, args);
	}

}
