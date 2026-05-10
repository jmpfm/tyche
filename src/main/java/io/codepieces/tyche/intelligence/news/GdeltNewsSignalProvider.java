package io.codepieces.tyche.intelligence.news;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import io.codepieces.tyche.assets.PortfolioPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class GdeltNewsSignalProvider implements NewsSignalProvider {

	private static final String SOURCE = "gdelt-doc-api";

	private final RestClient restClient;
	private final boolean enabled;
	private final String baseUrl;

	public GdeltNewsSignalProvider(
			@Value("${tyche.recommendations.news.enabled:true}") boolean enabled,
			@Value("${tyche.recommendations.news.gdelt-url:https://api.gdeltproject.org/api/v2/doc/doc}") String baseUrl
	) {
		this.restClient = RestClient.builder().requestFactory(requestFactory()).build();
		this.enabled = enabled;
		this.baseUrl = baseUrl;
	}

	@Override
	public NewsSignal newsFor(PortfolioPosition position) {
		if (!enabled || position.isCash()) {
			return NewsSignal.neutral(SOURCE);
		}

		try {
			JsonNode response = restClient.get()
					.uri(UriComponentsBuilder.fromUriString(baseUrl)
							.queryParam("query", "\"" + position.name() + "\" OR " + position.symbol())
							.queryParam("mode", "artlist")
							.queryParam("format", "json")
							.queryParam("maxrecords", "20")
							.queryParam("sort", "datedesc")
							.build()
							.encode()
							.toUri())
					.retrieve()
					.body(JsonNode.class);
			return parse(response);
		}
		catch (RuntimeException ex) {
			log.debug("GDELT news lookup failed for {}", position.symbol(), ex);
			return NewsSignal.neutral(SOURCE);
		}
	}

	private static NewsSignal parse(JsonNode response) {
		JsonNode articles = response == null ? null : response.path("articles");
		if (articles == null || !articles.isArray() || articles.isEmpty()) {
			return NewsSignal.neutral(SOURCE);
		}

		int positive = 0;
		int negative = 0;
		List<String> events = new ArrayList<>();

		for (JsonNode article : articles) {
			String title = article.path("title").asText("").toLowerCase(Locale.US);
			positive += keywordHits(title, "beats", "growth", "upgrade", "profit", "record", "expands", "approval");
			negative += keywordHits(title, "misses", "lawsuit", "probe", "downgrade", "loss", "layoff", "fraud", "warning");
			if (events.size() < 3 && !title.isBlank()) {
				events.add(article.path("title").asText());
			}
		}

		BigDecimal score = BigDecimal.valueOf(positive - negative)
				.divide(BigDecimal.valueOf(Math.max(articles.size(), 1)), 2, RoundingMode.HALF_UP)
				.max(new BigDecimal("-1.00"))
				.min(new BigDecimal("1.00"));
		return new NewsSignal(score, events, SOURCE, true);
	}

	private static int keywordHits(String text, String... keywords) {
		int hits = 0;
		for (String keyword : keywords) {
			if (text.contains(keyword)) {
				hits++;
			}
		}
		return hits;
	}

	private static SimpleClientHttpRequestFactory requestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));
		return requestFactory;
	}
}
