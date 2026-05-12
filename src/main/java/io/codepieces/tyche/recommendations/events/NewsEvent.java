package io.codepieces.tyche.recommendations.events;

import java.math.BigDecimal;
import java.time.Instant;

public record NewsEvent(
		String eventId,
		String symbol,
		String headline,
		String summary,
		BigDecimal score,
		String source,
		String url,
		String scope,
		Instant occurredAt
) {
}
