package io.codepieces.tyche.recommendations.model;

import java.time.Instant;
import java.util.List;

public record RecommendationRunResult(
		Instant generatedAt,
		int recommendationCount,
		String kafkaTopic,
		List<RecommendedTrade> recommendations
) {
}
