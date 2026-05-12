package io.codepieces.tyche.recommendations.state;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.codepieces.tyche.recommendations.config.RecommendationEngineProperties;
import io.codepieces.tyche.recommendations.events.NewsEvent;
import io.codepieces.tyche.recommendations.events.TechnicalAnalysisEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaRecommendationStateStore implements RecommendationStateStore {

	private static final String GLOBAL_KEY = "GLOBAL";
	private static final int NEWS_WINDOW = 20;
	private static final int EVENT_ID_WINDOW = 200;
	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().findAndAddModules().build();

	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RecommendationEngineProperties properties;

	private final Map<String, SymbolRecommendationState> symbolState = new ConcurrentHashMap<>();
	private final Map<String, Instant> changedAt = new ConcurrentHashMap<>();
	private final Deque<String> eventIdOrder = new ArrayDeque<>();
	private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();
	private volatile GlobalRecommendationState globalState = GlobalRecommendationState.empty();

	public KafkaRecommendationStateStore(
			KafkaTemplate<String, Object> kafkaTemplate,
			RecommendationEngineProperties properties
	) {
		this.kafkaTemplate = kafkaTemplate;
		this.properties = properties;
	}

	@Override
	public synchronized boolean saveTechnicalAnalysis(TechnicalAnalysisEvent event) {
		if (!registerEventId(event.eventId())) {
			return false;
		}

		String symbol = event.symbol().toUpperCase(Locale.US);
		SymbolRecommendationState existing = symbolState.getOrDefault(symbol, SymbolRecommendationState.empty(symbol));
		TechnicalAnalysisEvent previous = existing.technicalAnalysis();
		if (previous != null && !event.occurredAt().isAfter(previous.occurredAt())) {
			return false;
		}

		String hash = hashOf(event);
		if (hash.equals(existing.contentHash())) {
			return false;
		}

		SymbolRecommendationState updated = new SymbolRecommendationState(
				symbol,
				event,
				existing.symbolNews(),
				existing.stateVersion() + 1,
				Instant.now(),
				hash
		);
		symbolState.put(symbol, updated);
		changedAt.put(symbol, updated.lastChangedAt());
		persistSymbolState(updated);
		persistChange(symbol, updated.lastChangedAt());
		return true;
	}

	@Override
	public synchronized boolean saveNews(NewsEvent event) {
		if (!registerEventId(event.eventId())) {
			return false;
		}
		if (isGlobal(event)) {
			return saveGlobalNews(event);
		}

		String symbol = event.symbol().toUpperCase(Locale.US);
		SymbolRecommendationState existing = symbolState.getOrDefault(symbol, SymbolRecommendationState.empty(symbol));
		List<NewsEvent> updatedNews = prepend(event, existing.symbolNews(), NEWS_WINDOW);
		String hash = hashOf(updatedNews);
		if (hash.equals(existing.contentHash())) {
			return false;
		}

		SymbolRecommendationState updated = new SymbolRecommendationState(
				symbol,
				existing.technicalAnalysis(),
				updatedNews,
				existing.stateVersion() + 1,
				Instant.now(),
				hash
		);
		symbolState.put(symbol, updated);
		changedAt.put(symbol, updated.lastChangedAt());
		persistSymbolState(updated);
		persistChange(symbol, updated.lastChangedAt());
		return true;
	}

	@Override
	public Optional<SymbolRecommendationState> stateFor(String symbol) {
		return Optional.ofNullable(symbolState.get(symbol.toUpperCase(Locale.US)));
	}

	@Override
	public GlobalRecommendationState globalState() {
		return globalState;
	}

	@Override
	public Set<String> drainChangedSymbolsSince(Instant sinceExclusive) {
		return changedAt.entrySet().stream()
				.filter(entry -> entry.getValue().isAfter(sinceExclusive))
				.map(Map.Entry::getKey)
				.collect(java.util.stream.Collectors.toSet());
	}

	private boolean saveGlobalNews(NewsEvent event) {
		List<NewsEvent> updatedNews = prepend(event, globalState.news(), NEWS_WINDOW);
		String hash = hashOf(updatedNews);
		if (hash.equals(globalState.contentHash())) {
			return false;
		}
		GlobalRecommendationState updated = new GlobalRecommendationState(
				updatedNews,
				globalState.stateVersion() + 1,
				Instant.now(),
				hash
		);
		globalState = updated;
		persistGlobalState(updated);
		return true;
	}

	private void persistSymbolState(SymbolRecommendationState state) {
		kafkaTemplate.send(properties.state().technicalAnalysisTopic(), state.symbol(), state.technicalAnalysis());
		kafkaTemplate.send(properties.state().symbolNewsTopic(), state.symbol(), state.symbolNews());
	}

	private void persistGlobalState(GlobalRecommendationState state) {
		kafkaTemplate.send(properties.state().globalNewsTopic(), GLOBAL_KEY, state.news());
	}

	private void persistChange(String symbol, Instant changedAt) {
		kafkaTemplate.send(properties.state().changesTopic(), symbol, new StateChange(symbol, changedAt));
	}

	private static List<NewsEvent> prepend(NewsEvent event, List<NewsEvent> existing, int maxSize) {
		List<NewsEvent> copy = new ArrayList<>(maxSize);
		copy.add(event);
		for (NewsEvent newsEvent : existing) {
			if (copy.size() >= maxSize) {
				break;
			}
			copy.add(newsEvent);
		}
		return List.copyOf(copy);
	}

	private static boolean isGlobal(NewsEvent event) {
		return "GLOBAL".equalsIgnoreCase(event.scope()) || event.symbol() == null || event.symbol().isBlank();
	}

	private boolean registerEventId(String eventId) {
		if (eventId == null || eventId.isBlank()) {
			return true;
		}
		if (!processedEventIds.add(eventId)) {
			return false;
		}
		eventIdOrder.addLast(eventId);
		while (eventIdOrder.size() > EVENT_ID_WINDOW) {
			String evicted = eventIdOrder.removeFirst();
			processedEventIds.remove(evicted);
		}
		return true;
	}

	private String hashOf(Object value) {
		try {
			byte[] json = OBJECT_MAPPER.writeValueAsBytes(value);
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return Base64.getEncoder().encodeToString(digest.digest(json));
		}
		catch (JsonProcessingException | NoSuchAlgorithmException ex) {
			return Integer.toHexString(java.util.Arrays.hashCode(String.valueOf(value).getBytes(StandardCharsets.UTF_8)));
		}
	}

	private record StateChange(String symbol, Instant changedAt) {
	}
}
