package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record AssetIndicators(
		String symbol,
		BigDecimal rsi14,
		BigDecimal sma50,
		BigDecimal sma200,
		BigDecimal macd,
		BigDecimal macdSignal,
		String trend,
		String trendClass,
		String momentum,
		String momentumClass
) {

	private static final String NEUTRAL = "neutral";

	public static AssetIndicators unavailable(String symbol) {
		return new AssetIndicators(
				symbol,
				null,
				null,
				null,
				null,
				null,
				"N/A",
				NEUTRAL,
				"N/A",
				NEUTRAL
		);
	}

	public boolean available() {
		return rsi14 != null
				&& sma50 != null
				&& sma200 != null
				&& macd != null
				&& macdSignal != null;
	}

	public BigDecimal macdHistogram() {
		if (!available()) {
			return null;
		}
		return macd.subtract(macdSignal).setScale(2, RoundingMode.HALF_UP);
	}
}
