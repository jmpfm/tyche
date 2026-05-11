package io.codepieces.tyche.recommendations.delivery;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigDecimal;
import java.util.List;

import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import org.junit.jupiter.api.Test;

class KafkaTradeRecommendationPublisherTests {

	@Test
	void doesNotPublishWhenDisabled() {
		KafkaTradeRecommendationPublisher publisher = new KafkaTradeRecommendationPublisher(
				null,
				"tyche.trade-recommendations.v1",
				false
		);

		assertThatCode(() -> publisher.publish(List.of(
				new RecommendedTrade("Increase", "AAPL", "Apple Inc.", BigDecimal.ZERO, "rationale", "High")
		))).doesNotThrowAnyException();
	}

	@Test
	void doesNotPublishWhenRecommendationListEmpty() {
		KafkaTradeRecommendationPublisher publisher = new KafkaTradeRecommendationPublisher(
				null,
				"tyche.trade-recommendations.v1",
				true
		);

		assertThatCode(() -> publisher.publish(List.of())).doesNotThrowAnyException();
	}
}
