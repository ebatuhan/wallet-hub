package com.batu.ai_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AiAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiAssistantApplication.class, args);
	}

}
