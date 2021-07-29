package edu.stanford.protege.webprotege.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebprotegeGwtApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebprotegeGwtApiGatewayApplication.class, args);
	}

	@Bean
	DefaultMessageChannelMapper messageChannelMapper() {
		return new DefaultMessageChannelMapper();
	}
}
