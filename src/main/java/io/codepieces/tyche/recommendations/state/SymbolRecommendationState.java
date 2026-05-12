package io.codepieces.tyche.recommendations.state;

import java.time.Instant;
import java.util.List;

import io.codepieces.tyche.recommendations.events.NewsEvent;
import io.codepieces.tyche.recommendations.events.TechnicalAnalysisEvent;

public record SymbolRecommendationState(
		String symbol,
		TechnicalAnalysisEvent technicalAnalysis,
		List<NewsEvent> symbolNews,
		long stateVersion,
		Instant lastChangedAt,
		String contentHash
) {

	public static SymbolRecommendationState empty(String symbol) {
		return new SymbolRecommendationState(symbol, null, List.of(), 0L, Instant.EPOCH, "");
	}
}
