package io.codepieces.tyche.assets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.codepieces.tyche.analysis.TechnicalIndicatorService;
import io.codepieces.tyche.market.MarketDataService;
import io.codepieces.tyche.recommendations.scoring.TradeRecommendationService;

@WebMvcTest(AssetsController.class)
@Import({
		AssetMoneyFormatter.class,
		AssetPortfolioService.class,
		TradeRecommendationService.class,
		MarketDataService.class,
		TechnicalIndicatorService.class,
		AssetsControllerTests.PortfolioPositionTestConfiguration.class
})
class AssetsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void redirectsHomeToAssetsView() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/assets"));
	}

	@Test
	void rendersAssetsViewWithPortfolioSnapshot() throws Exception {
		mockMvc.perform(get("/assets"))
				.andExpect(status().isOk())
				.andExpect(view().name("assets/index"))
				.andExpect(model().attributeExists("portfolio"))
				.andExpect(result -> {
					AssetPortfolio portfolio = (AssetPortfolio) result.getModelAndView().getModel().get("portfolio");
					assertThat(portfolio.positions(), hasSize(5));
					assertThat(portfolio.recommendedTrades(), hasSize(3));
				})
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Assets")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Technical indicators")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Recommended trades")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("RSI 14")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("MACD")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Review")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("$36,395.00")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vanguard S&amp;P 500 ETF")));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class PortfolioPositionTestConfiguration {

		@Bean
		PortfolioPositionRepository portfolioPositionRepository() {
			return () -> List.of(
					position("VOO", "Vanguard S&P 500 ETF", "ETF", "18.0000", "425.30", "482.75", "0.42"),
					position("AAPL", "Apple Inc.", "Equity", "22.0000", "168.40", "186.90", "0.87"),
					position("MSFT", "Microsoft Corp.", "Equity", "10.0000", "391.20", "438.15", "-0.18"),
					position("BTC", "Bitcoin", "Crypto", "0.1850", "58250.00", "64120.00", "1.35"),
					position("CASH", "Available cash", "Cash", "1.0000", "7350.00", "7350.00", "0.00")
			);
		}

		private static PortfolioPosition position(
				String symbol,
				String name,
				String assetClass,
				String quantity,
				String averageCost,
				String marketPrice,
				String dayChangePercent
		) {
			return new PortfolioPosition(
					symbol,
					name,
					assetClass,
					new BigDecimal(quantity),
					new BigDecimal(averageCost),
					new BigDecimal(marketPrice),
					new BigDecimal(dayChangePercent)
			);
		}
	}
}
