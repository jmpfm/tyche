package io.codepieces.tyche.recommendations.workflow;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import io.codepieces.tyche.assets.AssetPortfolio;
import io.codepieces.tyche.assets.AssetPortfolioService;
import io.codepieces.tyche.recommendations.delivery.TradeRecommendationPublisher;
import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradeRecommendationWorkflow {

	private final AssetPortfolioService assetPortfolioService;
	private final TradeRecommendationPublisher publisher;
	private final Clock clock;
	private final String kafkaTopic;

	public TradeRecommendationWorkflow(
			AssetPortfolioService assetPortfolioService,
			TradeRecommendationPublisher publisher,
			@Value("${tyche.recommendations.kafka.topic:tyche.trade-recommendations.v1}") String kafkaTopic
	) {
		this.assetPortfolioService = assetPortfolioService;
		this.publisher = publisher;
		this.clock = Clock.systemUTC();
		this.kafkaTopic = kafkaTopic;
	}

	public RecommendationRunResult generateAndPublish() {
		AssetPortfolio portfolio = assetPortfolioService.currentPortfolio();
		List<RecommendedTrade> recommendations = portfolio.recommendedTrades();
		publisher.publish(recommendations);
		return new RecommendationRunResult(Instant.now(clock), recommendations.size(), kafkaTopic, recommendations);
	}
}
