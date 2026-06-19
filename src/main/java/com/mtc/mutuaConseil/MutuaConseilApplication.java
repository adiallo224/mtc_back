package com.mtc.mutuaConseil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class MutuaConseilApplication extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(MutuaConseilApplication.class);
	}

	public static void main(String[] args) {
		SpringApplication.run(MutuaConseilApplication.class, args);
	}

}
