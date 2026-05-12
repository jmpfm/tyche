package io.codepieces.tyche.recommendations.scoring;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.assets.AssetPosition;
import io.codepieces.tyche.recommendations.model.FactorScores;
import io.codepieces.tyche.recommendations.model.RecommendationSignal;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import org.springframework.stereotype.Service;

@Service
public class TradeRecommendationService {

	public static final String MODEL_VERSION = "hybrid-advisory-v1";

	private static final MathContext MONEY_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal CASH_RESERVE_PERCENT = new BigDecimal("10.00");
	private static final BigDecimal REBALANCE_THRESHOLD_PERCENT = new BigDecimal("2.00");
	private static final BigDecimal MIN_TRADE_AMOUNT = new BigDecimal("250.00");
	private static final BigDecimal MAX_SINGLE_POSITION_PERCENT = new BigDecimal("15.00");
	private static final BigDecimal TARGET_ADJUSTMENT_BAND = new BigDecimal("4.00");
	private static final BigDecimal RSI_OVERSOLD = new BigDecimal("35.00");
	private static final BigDecimal RSI_OVERBOUGHT = new BigDecimal("70.00");
	private static final BigDecimal SCORE_MIN = new BigDecimal("-1.00");
	private static final BigDecimal SCORE_MAX = new BigDecimal("1.00");
	private static final int MAX_RECOMMENDATIONS = 5;

	private final Clock clock;

	public TradeRecommendationService() {
		this(Clock.systemUTC());
	}

	TradeRecommendationService(Clock clock) {
		this.clock = clock;
	}

	public List<RecommendedTrade> recommendTrades(List<AssetPosition> positions, BigDecimal totalValue) {
		return recommendTrades(positions, totalValue, Map.of());
	}

	public List<RecommendedTrade> recommendTrades(
			List<AssetPosition> positions,
			BigDecimal totalValue,
			Map<String, RecommendationSignal> signals
	) {
		if (positions.isEmpty() || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
			return List.of();
		}

		BigDecimal deployableCash = deployableCash(positions, totalValue);
		List<RecommendedTrade> recommendations = new ArrayList<>();
		List<AssetPosition> investablePositions = positions.stream()
				.filter(position -> !isCash(position))
				.filter(TradeRecommendationService::majorExchangeAsset)
				.toList();

		for (AssetPosition position : investablePositions) {
			RecommendationSignal signal = signals.getOrDefault(position.symbol(), RecommendationSignal.neutral(position.symbol()));
			recommendationFor(position, totalValue, deployableCash, signal)
					.stream()
					.findFirst()
					.ifPresent(recommendations::add);
		}

		return recommendations.stream()
				.sorted(Comparator
						.comparing(TradeRecommendationService::actionPriority).reversed()
						.thenComparing(RecommendedTrade::confidenceScore).reversed()
						.thenComparing(RecommendedTrade::estimatedAmount).reversed())
				.limit(MAX_RECOMMENDATIONS)
				.toList();
	}

	public Optional<RecommendedTrade> recommendTradeForSymbol(
			AssetPosition position,
			BigDecimal totalValue,
			BigDecimal deployableCash,
			RecommendationSignal signal
	) {
		return recommendationFor(position, totalValue, deployableCash, signal).stream().findFirst();
	}

	public BigDecimal deployableCashFor(List<AssetPosition> positions, BigDecimal totalValue) {
		return deployableCash(positions, totalValue);
	}

	private List<RecommendedTrade> recommendationFor(
			AssetPosition position,
			BigDecimal totalValue,
			BigDecimal deployableCash,
			RecommendationSignal signal
	) {
		if (!dataQualityStrong(position)) {
			return List.of();
		}

		BigDecimal technicalScore = technicalScore(position.indicators());
		BigDecimal portfolioScore = portfolioScore(position);
		BigDecimal newsScore = clamp(signal.newsScore());
		BigDecimal macroScore = clamp(signal.macroScore());
		BigDecimal fundamentalsScore = clamp(signal.fundamentalsScore());
		BigDecimal compositeScore = weightedComposite(technicalScore, newsScore, macroScore, fundamentalsScore, portfolioScore);
		BigDecimal targetWeight = targetWeight(compositeScore);
		BigDecimal currentWeight = position.allocationPercent().setScale(2, RoundingMode.HALF_UP);
		BigDecimal weightDelta = targetWeight.subtract(currentWeight);
		FactorScores factorScores = new FactorScores(
				technicalScore,
				newsScore,
				macroScore,
				fundamentalsScore,
				portfolioScore,
				compositeScore
		);

		if (currentWeight.subtract(targetWeight).compareTo(REBALANCE_THRESHOLD_PERCENT) > 0) {
			return List.of(reduceRecommendation(position, totalValue, currentWeight, targetWeight, factorScores, signal));
		}

		if (weightDelta.compareTo(REBALANCE_THRESHOLD_PERCENT) > 0 && deployableCash.compareTo(MIN_TRADE_AMOUNT) >= 0) {
			BigDecimal deficitValue = allocationValue(totalValue, weightDelta);
			BigDecimal amount = deficitValue.min(deployableCash).setScale(2, RoundingMode.HALF_UP);
			if (amount.compareTo(MIN_TRADE_AMOUNT) >= 0) {
				return List.of(increaseRecommendation(position, currentWeight, targetWeight, amount, factorScores, signal));
			}
		}

		return List.of(holdRecommendation(position, currentWeight, targetWeight, factorScores, signal));
	}

	private RecommendedTrade reduceRecommendation(
			AssetPosition position,
			BigDecimal totalValue,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			FactorScores factorScores,
			RecommendationSignal signal
	) {
		BigDecimal overage = currentWeight.subtract(targetWeight);
		BigDecimal amount = allocationValue(totalValue, overage);
		BigDecimal quantityDelta = quantityDelta(amount.negate(), position.marketPrice());
		String rationale = rationale(
				"Reduce",
				position,
				currentWeight,
				targetWeight,
				factorScores,
				signal
		);
		return recommendation("Reduce", position, amount, rationale, currentWeight, targetWeight, quantityDelta, factorScores);
	}

	private RecommendedTrade increaseRecommendation(
			AssetPosition position,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			BigDecimal amount,
			FactorScores factorScores,
			RecommendationSignal signal
	) {
		BigDecimal quantityDelta = quantityDelta(amount, position.marketPrice());
		String rationale = rationale(
				"Increase",
				position,
				currentWeight,
				targetWeight,
				factorScores,
				signal
		);
		return recommendation("Increase", position, amount, rationale, currentWeight, targetWeight, quantityDelta, factorScores);
	}

	private RecommendedTrade holdRecommendation(
			AssetPosition position,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			FactorScores factorScores,
			RecommendationSignal signal
	) {
		String rationale = rationale(
				"Hold",
				position,
				currentWeight,
				targetWeight,
				factorScores,
				signal
		);
		return recommendation(
				"Hold",
				position,
				BigDecimal.ZERO.setScale(2),
				rationale,
				currentWeight,
				targetWeight,
				BigDecimal.ZERO.setScale(4),
				factorScores
		);
	}

	private RecommendedTrade recommendation(
			String action,
			AssetPosition position,
			BigDecimal amount,
			String rationale,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			BigDecimal quantityDelta,
			FactorScores factorScores
	) {
		BigDecimal confidenceScore = confidenceScore(action, currentWeight, targetWeight, factorScores);
		return new RecommendedTrade(
				action,
				position.symbol(),
				position.name(),
				amount.setScale(2, RoundingMode.HALF_UP),
				rationale,
				confidenceLabel(confidenceScore),
				confidenceScore,
				targetWeight,
				currentWeight,
				quantityDelta,
				"POSITION",
				factorScores,
				true,
				Instant.now(clock),
				MODEL_VERSION
		);
	}

	private static boolean majorExchangeAsset(AssetPosition position) {
		return "Equity".equalsIgnoreCase(position.assetClass()) || "ETF".equalsIgnoreCase(position.assetClass());
	}

	private static boolean dataQualityStrong(AssetPosition position) {
		return position.marketPrice().compareTo(BigDecimal.ZERO) > 0
				&& position.marketValue().compareTo(BigDecimal.ZERO) >= 0
				&& position.indicators() != null
				&& position.indicators().available();
	}

	private static BigDecimal technicalScore(AssetIndicators indicators) {
		if (indicators == null || !indicators.available()) {
			return BigDecimal.ZERO.setScale(2);
		}

		BigDecimal score = BigDecimal.ZERO;
		if ("bullish".equals(indicators.trendClass())) {
			score = score.add(new BigDecimal("0.45"));
		}
		if ("bearish".equals(indicators.trendClass())) {
			score = score.subtract(new BigDecimal("0.45"));
		}
		if ("bullish".equals(indicators.momentumClass())) {
			score = score.add(new BigDecimal("0.30"));
		}
		if ("bearish".equals(indicators.momentumClass())) {
			score = score.subtract(new BigDecimal("0.30"));
		}
		if (indicators.rsi14().compareTo(RSI_OVERSOLD) <= 0) {
			score = score.add(new BigDecimal("0.25"));
		}
		if (indicators.rsi14().compareTo(RSI_OVERBOUGHT) >= 0) {
			score = score.subtract(new BigDecimal("0.25"));
		}
		return clamp(score);
	}

	private static BigDecimal portfolioScore(AssetPosition position) {
		if (position.allocationPercent().compareTo(MAX_SINGLE_POSITION_PERCENT) > 0) {
			return new BigDecimal("-0.80");
		}
		if (position.allocationPercent().compareTo(MAX_SINGLE_POSITION_PERCENT.subtract(REBALANCE_THRESHOLD_PERCENT)) < 0) {
			return new BigDecimal("0.35");
		}
		return BigDecimal.ZERO.setScale(2);
	}

	private static BigDecimal weightedComposite(
			BigDecimal technical,
			BigDecimal news,
			BigDecimal macro,
			BigDecimal fundamentals,
			BigDecimal portfolio
	) {
		return clamp(technical.multiply(new BigDecimal("0.35"), MONEY_CONTEXT)
				.add(news.multiply(new BigDecimal("0.20"), MONEY_CONTEXT))
				.add(macro.multiply(new BigDecimal("0.10"), MONEY_CONTEXT))
				.add(fundamentals.multiply(new BigDecimal("0.25"), MONEY_CONTEXT))
				.add(portfolio.multiply(new BigDecimal("0.10"), MONEY_CONTEXT)))
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal targetWeight(BigDecimal compositeScore) {
		BigDecimal adjusted = MAX_SINGLE_POSITION_PERCENT.add(compositeScore.multiply(TARGET_ADJUSTMENT_BAND, MONEY_CONTEXT));
		if (adjusted.compareTo(BigDecimal.ZERO) < 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		if (adjusted.compareTo(MAX_SINGLE_POSITION_PERCENT) > 0) {
			return MAX_SINGLE_POSITION_PERCENT;
		}
		return adjusted.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal confidenceScore(
			String action,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			FactorScores factorScores
	) {
		BigDecimal deviation = currentWeight.subtract(targetWeight).abs();
		BigDecimal base = BigDecimal.valueOf(50)
				.add(deviation.multiply(new BigDecimal("3.00"), MONEY_CONTEXT))
				.add(factorScores.composite().abs().multiply(new BigDecimal("25.00"), MONEY_CONTEXT));
		if ("Hold".equals(action)) {
			base = BigDecimal.valueOf(45).add(factorScores.composite().abs().multiply(new BigDecimal("20.00"), MONEY_CONTEXT));
		}
		return base.min(new BigDecimal("95.00")).max(new BigDecimal("5.00")).setScale(2, RoundingMode.HALF_UP);
	}

	private static String confidenceLabel(BigDecimal confidenceScore) {
		if (confidenceScore.compareTo(new BigDecimal("75.00")) >= 0) {
			return "High";
		}
		if (confidenceScore.compareTo(new BigDecimal("50.00")) >= 0) {
			return "Medium";
		}
		return "Low";
	}

	private static String rationale(
			String action,
			AssetPosition position,
			BigDecimal currentWeight,
			BigDecimal targetWeight,
			FactorScores factorScores,
			RecommendationSignal signal
	) {
		String eventContext = signal.events().isEmpty()
				? "no material open-source event signal"
				: String.join("; ", signal.events());
		return String.format(
				Locale.US,
				"%s %s from %.2f%% toward %.2f%% target. Scores: TA %.2f, news %.2f, macro %.2f, fundamentals %.2f, portfolio %.2f. Context: %s.",
				action,
				position.symbol(),
				currentWeight,
				targetWeight,
				factorScores.technical(),
				factorScores.news(),
				factorScores.macro(),
				factorScores.fundamentals(),
				factorScores.portfolio(),
				eventContext
		);
	}

	private static BigDecimal deployableCash(List<AssetPosition> positions, BigDecimal totalValue) {
		BigDecimal cashValue = positions.stream()
				.filter(TradeRecommendationService::isCash)
				.map(AssetPosition::marketValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal requiredReserve = allocationValue(totalValue, CASH_RESERVE_PERCENT);
		return cashValue.subtract(requiredReserve).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	private static boolean isCash(AssetPosition position) {
		return "CASH".equalsIgnoreCase(position.symbol()) || "Cash".equalsIgnoreCase(position.assetClass());
	}

	private static BigDecimal allocationValue(BigDecimal value, BigDecimal percent) {
		return value
				.multiply(percent, MONEY_CONTEXT)
				.divide(ONE_HUNDRED, MONEY_CONTEXT)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal quantityDelta(BigDecimal amount, BigDecimal price) {
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			return BigDecimal.ZERO.setScale(4);
		}
		return amount.divide(price, 4, RoundingMode.HALF_UP);
	}

	private static BigDecimal clamp(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(2);
		}
		if (value.compareTo(SCORE_MIN) < 0) {
			return SCORE_MIN;
		}
		if (value.compareTo(SCORE_MAX) > 0) {
			return SCORE_MAX;
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private static int actionPriority(RecommendedTrade recommendation) {
		return switch (recommendation.action()) {
			case "Reduce", "Sell" -> 3;
			case "Increase", "Buy" -> 2;
			default -> 1;
		};
	}
}
