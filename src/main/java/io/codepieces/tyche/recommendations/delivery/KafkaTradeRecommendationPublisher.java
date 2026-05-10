package io.codepieces.tyche.recommendations.delivery;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import io.codepieces.tyche.recommendations.model.RecommendedTrade;

@Service
public class KafkaTradeRecommendationPublisher implements TradeRecommendationPublisher {

	private static final Logger log = LoggerFactory.getLogger(KafkaTradeRecommendationPublisher.class);

	private final KafkaTemplate<String, TradeRecommendationMessage> kafkaTemplate;
	private final String topic;
	private final boolean enabled;

	public KafkaTradeRecommendationPublisher(
			KafkaTemplate<String, TradeRecommendationMessage> kafkaTemplate,
			@Value("${tyche.recommendations.kafka.topic:tyche.trade-recommendations.v1}") String topic,
			@Value("${tyche.recommendations.kafka.enabled:true}") boolean enabled
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.enabled = enabled;
	}

	@Override
	public void publish(List<RecommendedTrade> recommendations) {
		if (!enabled || recommendations.isEmpty()) {
			return;
		}

		for (RecommendedTrade recommendation : recommendations) {
			TradeRecommendationMessage message = TradeRecommendationMessage.from(recommendation);
			kafkaTemplate.send(topic, recommendation.symbol(), message)
					.whenComplete((result, failure) -> {
						if (failure != null) {
							log.warn("Failed to publish trade recommendation for {}", recommendation.symbol(), failure);
						}
					});
		}
	}
}
