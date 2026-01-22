package tech.safepay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SafepayApplication {

	public static void main(String[] args) {
		SpringApplication.run(SafepayApplication.class, args);
	}

}
