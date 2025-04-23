package at.big5health.klimaatlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class KlimaatlasApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlimaatlasApplication.class, args);
	}

}
