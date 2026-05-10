package io.codepieces.tyche;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TycheApplication {

	public static void main(String[] args) {
		SpringApplication.run(TycheApplication.class, args);
	}

}
