package io.codepieces.tyche.recommendations.tracking;

public record TrackedStockRequest(
		String symbol,
		String name,
		Boolean enabled
) {
}
