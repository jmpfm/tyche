package io.codepieces.tyche.bdd;

import io.codepieces.tyche.recommendations.scoring.TradeRecommendationService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

@CucumberContextConfiguration
@ContextConfiguration(classes = CucumberSpringConfiguration.TestConfig.class)
class CucumberSpringConfiguration {

	@Configuration
	static class TestConfig {

		@Bean
		TradeRecommendationService tradeRecommendationService() {
			return new TradeRecommendationService();
		}
	}
}
