package com.HieuPahm.AniHoyo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class AniHoyoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AniHoyoApplication.class, args);
	}

}
