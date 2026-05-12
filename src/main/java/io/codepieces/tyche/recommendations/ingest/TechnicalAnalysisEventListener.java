package io.codepieces.tyche.recommendations.ingest;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.codepieces.tyche.recommendations.events.TechnicalAnalysisEvent;
import io.codepieces.tyche.recommendations.state.RecommendationStateStore;
import io.codepieces.tyche.recommendations.tracking.TrackedStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "tyche.recommendations.events", name = "enabled", havingValue = "true")
public class TechnicalAnalysisEventListener {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
	private final TrackedStockService trackedStockService;
	private final RecommendationStateStore recommendationStateStore;

	public TechnicalAnalysisEventListener(
			TrackedStockService trackedStockService,
			RecommendationStateStore recommendationStateStore
	) {
		this.trackedStockService = trackedStockService;
		this.recommendationStateStore = recommendationStateStore;
	}

	@KafkaListener(topics = "${tyche.recommendations.events.technical-analysis-topic}")
	public void onMessage(String payload) {
		try {
			TechnicalAnalysisEvent event = OBJECT_MAPPER.readValue(payload, TechnicalAnalysisEvent.class);
			String symbol = event.symbol() == null ? "" : event.symbol().trim().toUpperCase(Locale.US);
			if (symbol.isBlank() || !trackedStockService.isEnabledTrackedSymbol(symbol)) {
				return;
			}
			TechnicalAnalysisEvent normalized = new TechnicalAnalysisEvent(
					event.eventId() == null ? UUID.randomUUID().toString() : event.eventId(),
					symbol,
					event.rsi14(),
					event.sma50(),
					event.sma200(),
					event.macd(),
					event.macdSignal(),
					event.trendClass(),
					event.momentumClass(),
					event.source(),
					event.occurredAt() == null ? Instant.now() : event.occurredAt()
			);
			recommendationStateStore.saveTechnicalAnalysis(normalized);
		}
		catch (IOException ex) {
			log.warn("Failed to parse technical analysis event payload", ex);
		}
	}
}
