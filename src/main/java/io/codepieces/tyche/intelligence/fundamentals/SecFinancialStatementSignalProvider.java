package io.codepieces.tyche.intelligence.fundamentals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.JsonNode;
import io.codepieces.tyche.assets.PortfolioPosition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class SecFinancialStatementSignalProvider implements FinancialStatementSignalProvider {

	private static final String SOURCE = "sec-edgar-companyfacts";

	private final RestClient restClient;
	private final String userAgent;
	private final String tickersUrl;
	private final String companyFactsBaseUrl;
	private final Map<String, String> cikCache = new ConcurrentHashMap<>();

	public SecFinancialStatementSignalProvider(
			@Value("${tyche.recommendations.financial.sec.user-agent:}") String userAgent,
			@Value("${tyche.recommendations.financial.sec.tickers-url:https://www.sec.gov/files/company_tickers.json}") String tickersUrl,
			@Value("${tyche.recommendations.financial.sec.companyfacts-base-url:https://data.sec.gov/api/xbrl/companyfacts}") String companyFactsBaseUrl
	) {
		this.restClient = RestClient.builder().requestFactory(requestFactory()).build();
		this.userAgent = userAgent;
		this.tickersUrl = tickersUrl;
		this.companyFactsBaseUrl = companyFactsBaseUrl;
	}

	@Override
	public FinancialStatementSignal fundamentalsFor(PortfolioPosition position) {
		if (userAgent.isBlank() || position.isCash() || !"Equity".equalsIgnoreCase(position.assetClass())) {
			return FinancialStatementSignal.neutral(SOURCE);
		}

		try {
			String cik = cikFor(position.symbol());
			if (cik == null) {
				return FinancialStatementSignal.neutral(SOURCE);
			}
			JsonNode facts = restClient.get()
					.uri(companyFactsBaseUrl + "/CIK" + cik + ".json")
					.header("User-Agent", userAgent)
					.retrieve()
					.body(JsonNode.class);
			return parseFacts(facts);
		}
		catch (RuntimeException ex) {
			log.debug("SEC fundamentals lookup failed for {}", position.symbol(), ex);
			return FinancialStatementSignal.neutral(SOURCE);
		}
	}

	private String cikFor(String symbol) {
		String normalized = symbol.toUpperCase(Locale.US);
		if (cikCache.containsKey(normalized)) {
			return cikCache.get(normalized);
		}

		JsonNode tickers = restClient.get()
				.uri(tickersUrl)
				.header("User-Agent", userAgent)
				.retrieve()
				.body(JsonNode.class);
		if (tickers == null) {
			return null;
		}

		for (JsonNode company : tickers) {
			if (normalized.equals(company.path("ticker").asText("").toUpperCase(Locale.US))) {
				String cik = "%010d".formatted(company.path("cik_str").asLong());
				cikCache.put(normalized, cik);
				return cik;
			}
		}
		return null;
	}

	private static FinancialStatementSignal parseFacts(JsonNode facts) {
		List<AnnualMetric> revenue = annualMetric(facts, "Revenues");
		List<AnnualMetric> netIncome = annualMetric(facts, "NetIncomeLoss");
		if (revenue.size() < 2 || netIncome.isEmpty()) {
			return FinancialStatementSignal.neutral(SOURCE);
		}

		AnnualMetric latestRevenue = revenue.getLast();
		AnnualMetric priorRevenue = revenue.get(revenue.size() - 2);
		AnnualMetric latestIncome = netIncome.getLast();
		if (priorRevenue.value().compareTo(BigDecimal.ZERO) == 0 || latestRevenue.value().compareTo(BigDecimal.ZERO) == 0) {
			return FinancialStatementSignal.neutral(SOURCE);
		}

		BigDecimal revenueGrowth = latestRevenue.value().subtract(priorRevenue.value())
				.divide(priorRevenue.value().abs(), 4, RoundingMode.HALF_UP);
		BigDecimal netMargin = latestIncome.value().divide(latestRevenue.value(), 4, RoundingMode.HALF_UP);

		BigDecimal score = BigDecimal.ZERO;
		if (revenueGrowth.compareTo(new BigDecimal("0.05")) > 0) {
			score = score.add(new BigDecimal("0.35"));
		}
		if (revenueGrowth.compareTo(new BigDecimal("-0.05")) < 0) {
			score = score.subtract(new BigDecimal("0.35"));
		}
		if (netMargin.compareTo(new BigDecimal("0.10")) > 0) {
			score = score.add(new BigDecimal("0.35"));
		}
		if (netMargin.compareTo(BigDecimal.ZERO) < 0) {
			score = score.subtract(new BigDecimal("0.35"));
		}

		List<String> events = List.of(
				"Revenue growth " + revenueGrowth.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%",
				"Net margin " + netMargin.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) + "%"
		);
		return new FinancialStatementSignal(score.setScale(2, RoundingMode.HALF_UP), events, SOURCE, true);
	}

	private static List<AnnualMetric> annualMetric(JsonNode facts, String concept) {
		JsonNode values = facts == null
				? null
				: facts.path("facts").path("us-gaap").path(concept).path("units").path("USD");
		if (values == null || !values.isArray()) {
			return List.of();
		}

		List<AnnualMetric> annual = new ArrayList<>();
		for (JsonNode value : values) {
			if (!"10-K".equals(value.path("form").asText()) || !value.has("fy") || !value.has("val")) {
				continue;
			}
			annual.add(new AnnualMetric(value.path("fy").asInt(), value.path("val").decimalValue()));
		}
		return annual.stream()
				.sorted(Comparator.comparing(AnnualMetric::year))
				.toList();
	}

	private record AnnualMetric(int year, BigDecimal value) {
	}

	private static SimpleClientHttpRequestFactory requestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(3));
		return requestFactory;
	}
}
