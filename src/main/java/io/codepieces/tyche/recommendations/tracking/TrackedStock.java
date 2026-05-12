package io.codepieces.tyche.recommendations.tracking;

import java.time.Instant;

public record TrackedStock(
		String symbol,
		String name,
		boolean enabled,
		Instant createdAt,
		Instant updatedAt
) {
}
