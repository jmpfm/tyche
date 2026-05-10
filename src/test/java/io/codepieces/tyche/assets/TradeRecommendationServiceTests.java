package io.codepieces.tyche.assets;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class TradeRecommendationServiceTests {

	private final TradeRecommendationService tradeRecommendationService = new TradeRecommendationService();

	@Test
	void recommendsTrimmingConcentratedHoldingsAndDeployingExcessCash() {
		List<RecommendedTrade> trades = tradeRecommendationService.recommendTrades(List.of(
				position("VOO", "Vanguard S&P 500 ETF", "ETF", "8689.50", "23.88"),
				position("AAPL", "Apple Inc.", "Equity", "4111.80", "11.30"),
				position("MSFT", "Microsoft Corp.", "Equity", "4381.50", "12.04"),
				position("BTC", "Bitcoin", "Crypto", "11862.20", "32.59"),
				position("CASH", "Available cash", "Cash", "7350.00", "20.19")
		), dollars("36395.00"));

		assertThat(trades).hasSize(3);
		assertThat(trades.getFirst().action()).isEqualTo("Trim");
		assertThat(trades.getFirst().symbol()).isEqualTo("BTC");
		assertThat(trades.getFirst().estimatedAmount()).isEqualByComparingTo("1500.00");
		assertThat(trades.getFirst().confidence()).isEqualTo("High");
		assertThat(trades)
				.extracting(RecommendedTrade::symbol)
				.contains("VOO", "AAPL");
	}

	@Test
	void doesNotRecommendTradesInsideRebalanceThresholds() {
		List<RecommendedTrade> trades = tradeRecommendationService.recommendTrades(List.of(
				position("VOO", "Vanguard S&P 500 ETF", "ETF", "3500.00", "35.00"),
				position("AAPL", "Apple Inc.", "Equity", "1500.00", "15.00"),
				position("MSFT", "Microsoft Corp.", "Equity", "1500.00", "15.00"),
				position("BTC", "Bitcoin", "Crypto", "1000.00", "10.00"),
				position("BND", "Vanguard Total Bond Market ETF", "ETF", "1500.00", "15.00"),
				position("CASH", "Available cash", "Cash", "1000.00", "10.00")
		), dollars("10000.00"));

		assertThat(trades).isEmpty();
	}

	@Test
	void skipsBuysWhenCashReserveWouldBeBreached() {
		List<RecommendedTrade> trades = tradeRecommendationService.recommendTrades(List.of(
				position("VOO", "Vanguard S&P 500 ETF", "ETF", "3000.00", "30.00"),
				position("AAPL", "Apple Inc.", "Equity", "1500.00", "15.00"),
				position("MSFT", "Microsoft Corp.", "Equity", "1500.00", "15.00"),
				position("BTC", "Bitcoin", "Crypto", "1000.00", "10.00"),
				position("BND", "Vanguard Total Bond Market ETF", "ETF", "2500.00", "25.00"),
				position("CASH", "Available cash", "Cash", "500.00", "5.00")
		), dollars("10000.00"));

		assertThat(trades)
				.extracting(RecommendedTrade::action)
				.doesNotContain("Buy");
	}

	private static AssetPosition position(String symbol, String name, String assetClass, String marketValue, String allocationPercent) {
		return new AssetPosition(
				symbol,
				name,
				assetClass,
				BigDecimal.ONE,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				dollars(marketValue),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal(allocationPercent),
				allocationPercent + "%",
				AssetIndicators.unavailable(symbol)
		);
	}

	private static BigDecimal dollars(String value) {
		return new BigDecimal(value).setScale(2);
	}
}
