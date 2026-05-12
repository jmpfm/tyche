package io.codepieces.tyche.recommendations.state;

import java.time.Instant;
import java.util.List;

import io.codepieces.tyche.recommendations.events.NewsEvent;

public record GlobalRecommendationState(
		List<NewsEvent> news,
		long stateVersion,
		Instant lastChangedAt,
		String contentHash
) {

	public static GlobalRecommendationState empty() {
		return new GlobalRecommendationState(List.of(), 0L, Instant.EPOCH, "");
	}
}
