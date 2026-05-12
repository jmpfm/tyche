package io.codepieces.tyche.recommendations.ingest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.codepieces.tyche.recommendations.events.NewsEvent;
import io.codepieces.tyche.recommendations.state.RecommendationStateStore;
import io.codepieces.tyche.recommendations.tracking.TrackedStockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "tyche.recommendations.events", name = "enabled", havingValue = "true")
public class NewsEventListener {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();
	private final TrackedStockService trackedStockService;
	private final RecommendationStateStore recommendationStateStore;

	public NewsEventListener(
			TrackedStockService trackedStockService,
			RecommendationStateStore recommendationStateStore
	) {
		this.trackedStockService = trackedStockService;
		this.recommendationStateStore = recommendationStateStore;
	}

	@KafkaListener(topics = "${tyche.recommendations.events.news-topic}")
	public void onMessage(String payload) {
		try {
			NewsEvent event = OBJECT_MAPPER.readValue(payload, NewsEvent.class);
			String symbol = event.symbol() == null ? "" : event.symbol().trim().toUpperCase(Locale.US);
			boolean global = "GLOBAL".equalsIgnoreCase(event.scope()) || symbol.isBlank();
			if (!global && !trackedStockService.isEnabledTrackedSymbol(symbol)) {
				return;
			}

			NewsEvent normalized = new NewsEvent(
					event.eventId() == null ? UUID.randomUUID().toString() : event.eventId(),
					global ? "GLOBAL" : symbol,
					event.headline(),
					event.summary(),
					event.score() == null ? BigDecimal.ZERO.setScale(2) : event.score(),
					event.source(),
					event.url(),
					global ? "GLOBAL" : "SYMBOL",
					event.occurredAt() == null ? Instant.now() : event.occurredAt()
			);
			recommendationStateStore.saveNews(normalized);
		}
		catch (IOException ex) {
			log.warn("Failed to parse news event payload", ex);
		}
	}
}
