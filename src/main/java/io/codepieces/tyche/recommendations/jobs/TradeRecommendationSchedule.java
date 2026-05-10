package io.codepieces.tyche.recommendations.jobs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import io.codepieces.tyche.recommendations.workflow.TradeRecommendationWorkflow;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "tyche.recommendations.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TradeRecommendationSchedule {

	private final TradeRecommendationWorkflow workflow;

	public TradeRecommendationSchedule(TradeRecommendationWorkflow workflow) {
		this.workflow = workflow;
	}

	@Scheduled(
			cron = "${tyche.recommendations.schedule.cron:0 30 22 * * MON-FRI}",
			zone = "${tyche.recommendations.schedule.zone:Europe/London}"
	)
	public void generateScheduledRecommendations() {
		RecommendationRunResult result = workflow.generateAndPublish();
		log.info("Published {} advisory trade recommendations to {}", result.recommendationCount(), result.kafkaTopic());
	}
}
