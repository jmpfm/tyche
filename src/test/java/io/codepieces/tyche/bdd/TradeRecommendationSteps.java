package io.codepieces.tyche.bdd;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.assets.AssetPosition;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import io.codepieces.tyche.recommendations.scoring.TradeRecommendationService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TradeRecommendationSteps {

	private final TradeRecommendationService tradeRecommendationService;

	private BigDecimal totalValue;
	private List<AssetPosition> positions;
	private List<RecommendedTrade> recommendations;

	public TradeRecommendationSteps(TradeRecommendationService tradeRecommendationService) {
		this.tradeRecommendationService = tradeRecommendationService;
	}

	@Given("a portfolio worth {string} with positions")
	public void aPortfolioWorthWithPositions(String totalValue, DataTable table) {
		this.totalValue = dollars(totalValue);
		this.positions = table.asMaps().stream()
				.map(TradeRecommendationSteps::position)
				.toList();
	}

	@When("recommendations are generated")
	public void recommendationsAreGenerated() {
		recommendations = tradeRecommendationService.recommendTrades(positions, totalValue);
	}

	@Then("the first recommendation should be to {string} {string}")
	public void theFirstRecommendationShouldBeTo(String action, String symbol) {
		assertThat(recommendations).isNotEmpty();
		assertThat(recommendations.getFirst().action()).isEqualTo(action);
		assertThat(recommendations.getFirst().symbol()).isEqualTo(symbol);
	}

	@Then("the estimated amount should be {string}")
	public void theEstimatedAmountShouldBe(String estimatedAmount) {
		assertThat(recommendations.getFirst().estimatedAmount()).isEqualByComparingTo(estimatedAmount);
	}

	@Then("the confidence should be {string}")
	public void theConfidenceShouldBe(String confidence) {
		assertThat(recommendations.getFirst().confidence()).isEqualTo(confidence);
	}

	private static AssetPosition position(Map<String, String> row) {
		String symbol = row.get("symbol");
		String allocationPercent = row.get("allocationPercent");
		BigDecimal marketValue = dollars(row.get("marketValue"));
		return new AssetPosition(
				symbol,
				row.get("name"),
				row.get("assetClass"),
				BigDecimal.ONE,
				BigDecimal.ZERO,
				marketValue,
				marketValue,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal(allocationPercent),
				allocationPercent + "%",
				availableIndicators(symbol)
		);
	}

	private static BigDecimal dollars(String value) {
		return new BigDecimal(value).setScale(2);
	}

	private static AssetIndicators availableIndicators(String symbol) {
		if ("CASH".equals(symbol)) {
			return AssetIndicators.unavailable(symbol);
		}
		return new AssetIndicators(
				symbol,
				new BigDecimal("55.00"),
				new BigDecimal("95.00"),
				new BigDecimal("90.00"),
				new BigDecimal("1.00"),
				new BigDecimal("0.50"),
				"Uptrend",
				"bullish",
				"Positive",
				"bullish"
		);
	}
}
