package com.batu.dashboard_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import com.batu.dashboard_service.config.DashboardFeignConfiguration;

@SpringBootApplication
@EnableFeignClients(defaultConfiguration = DashboardFeignConfiguration.class)
public class DashboardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DashboardServiceApplication.class, args);
	}

}
