package io.codepieces.tyche.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.market.MarketBar;
import org.junit.jupiter.api.Test;

class TechnicalIndicatorServiceTests {

	private final TechnicalIndicatorService technicalIndicatorService = new TechnicalIndicatorService();

	@Test
	void calculatesIndicatorsFromDailyBars() {
		AssetIndicators indicators = technicalIndicatorService.indicatorsFor("TEST", ascendingHistory(220));

		assertThat(indicators.available()).isTrue();
		assertThat(indicators.rsi14()).isEqualByComparingTo("100.00");
		assertThat(indicators.sma50()).isEqualByComparingTo("195.50");
		assertThat(indicators.sma200()).isEqualByComparingTo("120.50");
		assertThat(indicators.macd()).isGreaterThan(BigDecimal.ZERO);
		assertThat(indicators.macdSignal()).isGreaterThan(BigDecimal.ZERO);
		assertThat(indicators.trend()).isEqualTo("Uptrend");
		assertThat(indicators.trendClass()).isEqualTo("bullish");
		assertThat(indicators.momentumClass()).isEqualTo("bearish");
	}

	@Test
	void marksIndicatorsUnavailableWhenHistoryIsInsufficient() {
		AssetIndicators indicators = technicalIndicatorService.indicatorsFor("TEST", ascendingHistory(20));

		assertThat(indicators.available()).isFalse();
		assertThat(indicators.trend()).isEqualTo("N/A");
		assertThat(indicators.momentum()).isEqualTo("N/A");
	}

	private static List<MarketBar> ascendingHistory(int bars) {
		return IntStream.rangeClosed(1, bars)
				.mapToObj(TechnicalIndicatorServiceTests::bar)
				.toList();
	}

	private static MarketBar bar(int close) {
		BigDecimal price = BigDecimal.valueOf(close).setScale(2);
		return new MarketBar(
				LocalDate.of(2025, 1, 1).plusDays(close - 1L),
				price,
				price,
				price,
				price,
				BigDecimal.valueOf(1_000_000)
		);
	}
}
