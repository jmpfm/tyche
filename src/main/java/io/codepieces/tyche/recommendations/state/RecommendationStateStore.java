package io.codepieces.tyche.recommendations.state;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import io.codepieces.tyche.recommendations.events.NewsEvent;
import io.codepieces.tyche.recommendations.events.TechnicalAnalysisEvent;

public interface RecommendationStateStore {

	boolean saveTechnicalAnalysis(TechnicalAnalysisEvent event);

	boolean saveNews(NewsEvent event);

	Optional<SymbolRecommendationState> stateFor(String symbol);

	GlobalRecommendationState globalState();

	Set<String> drainChangedSymbolsSince(Instant sinceExclusive);
}
