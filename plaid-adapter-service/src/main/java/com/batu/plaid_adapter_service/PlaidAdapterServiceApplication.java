package com.batu.plaid_adapter_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlaidAdapterServiceApplication {

	public static void main(String[] args) {

		SpringApplication.run(PlaidAdapterServiceApplication.class, args);

	}

}
