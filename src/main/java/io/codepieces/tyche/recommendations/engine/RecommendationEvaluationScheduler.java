package io.codepieces.tyche.recommendations.engine;

import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "tyche.recommendations.engine", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RecommendationEvaluationScheduler {

	private final TradingRecommendationEngine tradingRecommendationEngine;

	public RecommendationEvaluationScheduler(TradingRecommendationEngine tradingRecommendationEngine) {
		this.tradingRecommendationEngine = tradingRecommendationEngine;
	}

	@Scheduled(fixedDelayString = "${tyche.recommendations.engine.interval:PT1H}")
	public void evaluateChangedStates() {
		RecommendationRunResult result = tradingRecommendationEngine.evaluateChangedSymbols();
		log.info("Published {} event-driven advisory recommendations", result.recommendationCount());
	}
}
