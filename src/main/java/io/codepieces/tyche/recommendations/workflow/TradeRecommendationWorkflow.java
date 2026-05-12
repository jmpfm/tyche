package io.codepieces.tyche.recommendations.workflow;

import java.time.Clock;
import java.time.Instant;

import io.codepieces.tyche.recommendations.engine.TradingRecommendationEngine;
import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradeRecommendationWorkflow {

	private final TradingRecommendationEngine tradingRecommendationEngine;
	private final Clock clock;
	private final String kafkaTopic;

	public TradeRecommendationWorkflow(
			TradingRecommendationEngine tradingRecommendationEngine,
			@Value("${tyche.recommendations.kafka.topic:tyche.trade-recommendations.v1}") String kafkaTopic
	) {
		this.tradingRecommendationEngine = tradingRecommendationEngine;
		this.clock = Clock.systemUTC();
		this.kafkaTopic = kafkaTopic;
	}

	public RecommendationRunResult generateAndPublish() {
		RecommendationRunResult result = tradingRecommendationEngine.evaluateNow();
		return new RecommendationRunResult(Instant.now(clock), result.recommendationCount(), kafkaTopic, result.recommendations());
	}
}
