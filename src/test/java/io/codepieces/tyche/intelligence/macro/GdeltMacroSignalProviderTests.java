package io.codepieces.tyche.intelligence.macro;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GdeltMacroSignalProviderTests {

	@Test
	void parsesScoredMacroSignalFromGdeltArticles() throws Exception {
		MacroSignal signal = parse("""
				{
				  "articles": [
				    {"title": "Apple reports record profit growth after major upgrade"},
				    {"title": "Microsoft expands cloud partnerships as demand grows"},
				    {"title": "Inflation concerns rise as central bank signals hikes rates"}
				  ]
				}
				""");

		assertThat(signal.available()).isTrue();
		assertThat(signal.source()).isEqualTo("gdelt-world-developments");
		assertThat(signal.score()).isEqualByComparingTo("-0.33");
		assertThat(signal.events()).hasSize(3);
	}

	@Test
	void returnsNeutralSignalWhenProviderDisabled() {
		GdeltMacroSignalProvider provider = new GdeltMacroSignalProvider(false, "http://127.0.0.1:1/api/v2/doc/doc");
		MacroSignal signal = provider.currentMacroSignal();

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	@Test
	void returnsNeutralSignalForEmptyArticles() throws Exception {
		MacroSignal signal = parse("{\"articles\": []}");

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	private static MacroSignal parse(String json) throws Exception {
		Method parse = GdeltMacroSignalProvider.class.getDeclaredMethod("parse", JsonNode.class);
		parse.setAccessible(true);
		return (MacroSignal) parse.invoke(null, new ObjectMapper().readTree(json));
	}
}
