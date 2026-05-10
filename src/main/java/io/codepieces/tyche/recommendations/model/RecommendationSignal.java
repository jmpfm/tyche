package io.codepieces.tyche.recommendations.model;

import java.math.BigDecimal;
import java.util.List;

public record RecommendationSignal(
		String symbol,
		BigDecimal newsScore,
		BigDecimal macroScore,
		BigDecimal fundamentalsScore,
		List<String> events,
		List<String> dataSources,
		boolean externalDataAvailable
) {

	private static final BigDecimal NEUTRAL = BigDecimal.ZERO.setScale(2);

	public static RecommendationSignal neutral(String symbol) {
		return new RecommendationSignal(
				symbol,
				NEUTRAL,
				NEUTRAL,
				NEUTRAL,
				List.of(),
				List.of("portfolio", "technical-analysis"),
				false
		);
	}
}
