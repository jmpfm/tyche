package io.codepieces.tyche.intelligence.fundamentals;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codepieces.tyche.assets.PortfolioPosition;
import org.junit.jupiter.api.Test;

class SecFinancialStatementSignalProviderTests {

	@Test
	void parsesFundamentalScoreFromCompanyFacts() throws Exception {
		FinancialStatementSignal signal = parseFacts("""
				{
				  "facts": {
				    "us-gaap": {
				      "Revenues": {
				        "units": {
				          "USD": [
				            {"form": "10-K", "fy": 2023, "val": 383285000000},
				            {"form": "10-K", "fy": 2024, "val": 401120000000}
				          ]
				        }
				      },
				      "NetIncomeLoss": {
				        "units": {
				          "USD": [
				            {"form": "10-K", "fy": 2024, "val": 109210000000}
				          ]
				        }
				      }
				    }
				  }
				}
				""");

		assertThat(signal.available()).isTrue();
		assertThat(signal.source()).isEqualTo("sec-edgar-companyfacts");
		assertThat(signal.score()).isEqualByComparingTo("0.35");
		assertThat(signal.events()).contains("Revenue growth 4.65%", "Net margin 27.23%");
	}

	@Test
	void returnsNeutralSignalWhenSecUserAgentMissing() {
		SecFinancialStatementSignalProvider provider = new SecFinancialStatementSignalProvider(
				"",
				"http://127.0.0.1:1/files/company_tickers.json",
				"http://127.0.0.1:1/api/xbrl/companyfacts"
		);

		FinancialStatementSignal signal = provider.fundamentalsFor(position("AAPL", "Apple Inc.", "Equity"));

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	@Test
	void returnsNeutralWhenFactsAreIncomplete() throws Exception {
		FinancialStatementSignal signal = parseFacts("""
				{
				  "facts": {
				    "us-gaap": {
				      "Revenues": {
				        "units": {
				          "USD": [{"form": "10-K", "fy": 2024, "val": 401120000000}]
				        }
				      }
				    }
				  }
				}
				""");

		assertThat(signal.available()).isFalse();
		assertThat(signal.score()).isEqualTo(BigDecimal.ZERO.setScale(2));
		assertThat(signal.events()).isEmpty();
	}

	private static FinancialStatementSignal parseFacts(String json) throws Exception {
		Method parseFacts = SecFinancialStatementSignalProvider.class.getDeclaredMethod("parseFacts", JsonNode.class);
		parseFacts.setAccessible(true);
		return (FinancialStatementSignal) parseFacts.invoke(null, new ObjectMapper().readTree(json));
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
