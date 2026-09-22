package com.example.treasury;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TreasuryApplication {

	public static void main(String[] args) {
		SpringApplication.run(TreasuryApplication.class, args);
	}

}
