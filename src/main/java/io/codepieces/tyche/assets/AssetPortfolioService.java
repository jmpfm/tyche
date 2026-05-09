package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AssetPortfolioService {

	private static final MathContext MONEY_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	public AssetPortfolio currentPortfolio() {
		List<RawPosition> rawPositions = List.of(
				new RawPosition("VOO", "Vanguard S&P 500 ETF", "ETF", "18.0000", "425.30", "482.75", "0.42"),
				new RawPosition("AAPL", "Apple Inc.", "Equity", "22.0000", "168.40", "186.90", "0.87"),
				new RawPosition("MSFT", "Microsoft Corp.", "Equity", "10.0000", "391.20", "438.15", "-0.18"),
				new RawPosition("BTC", "Bitcoin", "Crypto", "0.1850", "58250.00", "64120.00", "1.35"),
				new RawPosition("CASH", "Available cash", "Cash", "1.0000", "7350.00", "7350.00", "0.00")
		);

		BigDecimal totalValue = rawPositions.stream()
				.map(RawPosition::marketValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCost = rawPositions.stream()
				.map(RawPosition::costBasis)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unrealizedProfitLoss = totalValue.subtract(totalCost);
		BigDecimal dayChangeValue = rawPositions.stream()
				.map(RawPosition::dayChangeValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<AssetPosition> positions = rawPositions.stream()
				.map(position -> position.toAssetPosition(totalValue))
				.toList();

		return new AssetPortfolio(
				money(totalValue),
				money(totalCost),
				money(unrealizedProfitLoss),
				percent(unrealizedProfitLoss, totalCost),
				money(dayChangeValue),
				percent(dayChangeValue, totalValue.subtract(dayChangeValue)),
				positions,
				recommendedTrades()
		);
	}

	private static List<RecommendedTrade> recommendedTrades() {
		return List.of(
				new RecommendedTrade(
						"Buy",
						"VOO",
						"Vanguard S&P 500 ETF",
						money(new BigDecimal("1250.00")),
						"Increase broad-market exposure using available cash.",
						"High"
				),
				new RecommendedTrade(
						"Trim",
						"BTC",
						"Bitcoin",
						money(new BigDecimal("900.00")),
						"Reduce crypto concentration after recent outperformance.",
						"Medium"
				),
				new RecommendedTrade(
						"Buy",
						"MSFT",
						"Microsoft Corp.",
						money(new BigDecimal("750.00")),
						"Add to software allocation while daily momentum is muted.",
						"Medium"
				)
		);
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
		if (denominator.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		return numerator
				.divide(denominator, MONEY_CONTEXT)
				.multiply(ONE_HUNDRED)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private record RawPosition(
			String symbol,
			String name,
			String assetClass,
			String quantity,
			String averageCost,
			String marketPrice,
			String dayChangePercent
	) {

		private BigDecimal quantityValue() {
			return new BigDecimal(quantity);
		}

		private BigDecimal averageCostValue() {
			return new BigDecimal(averageCost);
		}

		private BigDecimal marketPriceValue() {
			return new BigDecimal(marketPrice);
		}

		private BigDecimal dayChangePercentValue() {
			return new BigDecimal(dayChangePercent);
		}

		private BigDecimal costBasis() {
			return quantityValue().multiply(averageCostValue(), MONEY_CONTEXT);
		}

		private BigDecimal marketValue() {
			return quantityValue().multiply(marketPriceValue(), MONEY_CONTEXT);
		}

		private BigDecimal dayChangeValue() {
			return marketValue()
					.multiply(dayChangePercentValue(), MONEY_CONTEXT)
					.divide(ONE_HUNDRED, MONEY_CONTEXT);
		}

		private AssetPosition toAssetPosition(BigDecimal totalValue) {
			BigDecimal marketValue = marketValue();
			BigDecimal costBasis = costBasis();
			BigDecimal unrealizedProfitLoss = marketValue.subtract(costBasis);
			BigDecimal allocationPercent = percent(marketValue, totalValue);

			return new AssetPosition(
					symbol,
					name,
					assetClass,
					quantityValue(),
					money(averageCostValue()),
					money(marketPriceValue()),
					money(marketValue),
					money(unrealizedProfitLoss),
					percent(unrealizedProfitLoss, costBasis),
					dayChangePercentValue().setScale(2, RoundingMode.HALF_UP),
					allocationPercent,
					allocationPercent.max(BigDecimal.ONE).toPlainString() + "%"
			);
		}
	}
}
