package io.codepieces.tyche.recommendations.events;

import java.math.BigDecimal;
import java.time.Instant;

public record TechnicalAnalysisEvent(
		String eventId,
		String symbol,
		BigDecimal rsi14,
		BigDecimal sma50,
		BigDecimal sma200,
		BigDecimal macd,
		BigDecimal macdSignal,
		String trendClass,
		String momentumClass,
		String source,
		Instant occurredAt
) {
}
