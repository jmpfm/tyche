package io.codepieces.tyche.recommendations.model;

import java.math.BigDecimal;
import java.time.Instant;

public record RecommendedTrade(
		String action,
		String symbol,
		String name,
		BigDecimal estimatedAmount,
		String rationale,
		String confidence,
		BigDecimal confidenceScore,
		BigDecimal targetPortfolioWeight,
		BigDecimal currentPortfolioWeight,
		BigDecimal suggestedQuantityDelta,
		String horizon,
		FactorScores factorScores,
		boolean dataQualityStrong,
		Instant generatedAt,
		String modelVersion
) {

	public RecommendedTrade(
			String action,
			String symbol,
			String name,
			BigDecimal estimatedAmount,
			String rationale,
			String confidence
	) {
		this(
				action,
				symbol,
				name,
				estimatedAmount,
				rationale,
				confidence,
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(2),
				BigDecimal.ZERO.setScale(4),
				"POSITION",
				new FactorScores(
						BigDecimal.ZERO.setScale(2),
						BigDecimal.ZERO.setScale(2),
						BigDecimal.ZERO.setScale(2),
						BigDecimal.ZERO.setScale(2),
						BigDecimal.ZERO.setScale(2),
						BigDecimal.ZERO.setScale(2)
				),
				false,
				Instant.EPOCH,
				"legacy"
		);
	}
}
