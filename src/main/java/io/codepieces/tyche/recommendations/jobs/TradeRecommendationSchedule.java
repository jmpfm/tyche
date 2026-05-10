package io.codepieces.tyche.recommendations.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import io.codepieces.tyche.recommendations.workflow.TradeRecommendationWorkflow;

@Component
@ConditionalOnProperty(prefix = "tyche.recommendations.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TradeRecommendationSchedule {

	private static final Logger log = LoggerFactory.getLogger(TradeRecommendationSchedule.class);

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
