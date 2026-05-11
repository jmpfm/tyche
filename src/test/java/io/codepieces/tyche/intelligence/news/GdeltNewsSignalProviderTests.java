package io.codepieces.tyche.intelligence.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codepieces.tyche.assets.PortfolioPosition;
import org.junit.jupiter.api.Test;

class GdeltNewsSignalProviderTests {

	@Test
	void parsesScoredNewsSignalFromGdeltArticles() throws Exception {
		NewsSignal signal = parse("""
				{
				  "articles": [
				    {"title": "Apple reports record profit growth after major upgrade"},
				    {"title": "Microsoft expands cloud partnerships as demand grows"},
				    {"title": "Inflation concerns rise as central bank signals hikes rates"}
				  ]
				}
				""");

		assertThat(signal.available()).isTrue();
		assertThat(signal.source()).isEqualTo("gdelt-doc-api");
		assertThat(signal.score()).isEqualByComparingTo("1.00");
		assertThat(signal.events()).hasSize(3);
	}

	@Test
	void returnsNeutralSignalWhenProviderDisabled() {
		GdeltNewsSignalProvider provider = new GdeltNewsSignalProvider(false, "http://127.0.0.1:1/api/v2/doc/doc");
		NewsSignal signal = provider.newsFor(position("AAPL", "Apple Inc.", "Equity"));

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	@Test
	void returnsNeutralSignalForEmptyArticles() throws Exception {
		NewsSignal signal = parse("{\"articles\": []}");

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	private static NewsSignal parse(String json) throws Exception {
		Method parse = GdeltNewsSignalProvider.class.getDeclaredMethod("parse", com.fasterxml.jackson.databind.JsonNode.class);
		parse.setAccessible(true);
		return (NewsSignal) parse.invoke(null, new ObjectMapper().readTree(json));
	}

	private static PortfolioPosition position(String symbol, String name, String assetClass) {
		return new PortfolioPosition(
				symbol,
				name,
				assetClass,
				BigDecimal.ONE,
				BigDecimal.ONE,
				BigDecimal.ONE,
				BigDecimal.ZERO
		);
	}
}
