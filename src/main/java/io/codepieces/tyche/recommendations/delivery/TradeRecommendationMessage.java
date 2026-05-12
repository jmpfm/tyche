package io.codepieces.tyche.recommendations.delivery;

import java.math.BigDecimal;
import java.time.Instant;

import io.codepieces.tyche.recommendations.model.FactorScores;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;

public record TradeRecommendationMessage(
		String schemaVersion,
		String recommendationId,
		String action,
		String symbol,
		String name,
		BigDecimal estimatedAmount,
		BigDecimal suggestedQuantityDelta,
		BigDecimal currentPortfolioWeight,
		BigDecimal targetPortfolioWeight,
		String horizon,
		String confidence,
		BigDecimal confidenceScore,
		FactorScores factorScores,
		String rationale,
		boolean advisoryOnly,
		boolean dataQualityStrong,
		String triggerType,
		Instant triggerWindowStartedAt,
		Instant triggerWindowEndedAt,
		Instant generatedAt,
		String modelVersion
) {

	public static TradeRecommendationMessage from(RecommendedTrade recommendation) {
		return new TradeRecommendationMessage(
				"1.0",
				recommendation.symbol() + "-" + recommendation.generatedAt().toEpochMilli(),
				recommendation.action(),
				recommendation.symbol(),
				recommendation.name(),
				recommendation.estimatedAmount(),
				recommendation.suggestedQuantityDelta(),
				recommendation.currentPortfolioWeight(),
				recommendation.targetPortfolioWeight(),
				recommendation.horizon(),
				recommendation.confidence(),
				recommendation.confidenceScore(),
				recommendation.factorScores(),
				recommendation.rationale(),
				true,
				recommendation.dataQualityStrong(),
				"HOURLY_STATE_EVALUATION",
				recommendation.generatedAt(),
				recommendation.generatedAt(),
				recommendation.generatedAt(),
				recommendation.modelVersion()
		);
	}
}
