package io.codepieces.tyche.recommendations.delivery;

import java.util.List;

import io.codepieces.tyche.recommendations.model.RecommendedTrade;

public interface TradeRecommendationPublisher {

	void publish(List<RecommendedTrade> recommendations);
}
