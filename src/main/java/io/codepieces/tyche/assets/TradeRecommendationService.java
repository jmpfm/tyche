package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class TradeRecommendationService {

	private static final MathContext MONEY_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final BigDecimal CASH_RESERVE_PERCENT = new BigDecimal("10.00");
	private static final BigDecimal REBALANCE_THRESHOLD_PERCENT = new BigDecimal("2.00");
	private static final BigDecimal MIN_TRADE_AMOUNT = new BigDecimal("250.00");
	private static final BigDecimal MAX_TRADE_AMOUNT = new BigDecimal("1500.00");
	private static final BigDecimal MAX_TRIM_POSITION_PERCENT = new BigDecimal("15.00");
	private static final BigDecimal TRADE_INCREMENT = new BigDecimal("50.00");
	private static final BigDecimal RSI_OVERSOLD = new BigDecimal("35.00");
	private static final BigDecimal RSI_OVERBOUGHT = new BigDecimal("70.00");
	private static final int MAX_RECOMMENDATIONS = 3;
	private static final Map<String, BigDecimal> TARGET_ALLOCATIONS = Map.of(
			"VOO", new BigDecimal("35.00"),
			"AAPL", new BigDecimal("15.00"),
			"MSFT", new BigDecimal("15.00"),
			"BTC", new BigDecimal("10.00")
	);

	public List<RecommendedTrade> recommendTrades(List<AssetPosition> positions, BigDecimal totalValue) {
		if (positions.isEmpty() || totalValue.compareTo(BigDecimal.ZERO) <= 0) {
			return List.of();
		}

		List<TradeCandidate> trimCandidates = positions.stream()
				.filter(position -> !isCash(position))
				.map(position -> trimCandidate(position, totalValue))
				.flatMap(Optional::stream)
				.sorted(Comparator.comparing(TradeCandidate::priority).reversed())
				.toList();

		BigDecimal deployableCash = deployableCash(positions, totalValue);
		List<TradeCandidate> buyCandidates = buyCandidates(positions, totalValue, deployableCash);

		List<TradeCandidate> candidates = new ArrayList<>();
		candidates.addAll(trimCandidates);
		candidates.addAll(buyCandidates);

		return candidates.stream()
				.sorted(Comparator.comparing(TradeCandidate::priority).reversed())
				.limit(MAX_RECOMMENDATIONS)
				.map(TradeCandidate::toRecommendedTrade)
				.toList();
	}

	private Optional<TradeCandidate> trimCandidate(AssetPosition position, BigDecimal totalValue) {
		BigDecimal targetAllocation = targetAllocation(position);
		if (targetAllocation.compareTo(BigDecimal.ZERO) == 0) {
			return Optional.empty();
		}

		BigDecimal overagePercent = position.allocationPercent().subtract(targetAllocation);
		if (overagePercent.compareTo(REBALANCE_THRESHOLD_PERCENT) <= 0) {
			return Optional.empty();
		}

		BigDecimal overageValue = allocationValue(totalValue, overagePercent);
		BigDecimal positionTrimLimit = allocationValue(position.marketValue(), MAX_TRIM_POSITION_PERCENT);
		BigDecimal estimatedAmount = roundTradeAmount(overageValue.min(positionTrimLimit).min(MAX_TRADE_AMOUNT));
		if (estimatedAmount.compareTo(MIN_TRADE_AMOUNT) < 0) {
			return Optional.empty();
		}

		String rationale = String.format(
				Locale.US,
				"Reduce concentration from %.2f%% toward %.2f%% target allocation.",
				position.allocationPercent(),
				targetAllocation
		);
		return Optional.of(new TradeCandidate(
				"Trim",
				position,
				estimatedAmount,
				withTechnicalContext(rationale, position),
				confidence(overagePercent, position, "Trim"),
				overagePercent
		));
	}

	private List<TradeCandidate> buyCandidates(List<AssetPosition> positions, BigDecimal totalValue, BigDecimal deployableCash) {
		if (deployableCash.compareTo(MIN_TRADE_AMOUNT) < 0) {
			return List.of();
		}

		BigDecimal remainingCash = deployableCash;
		List<TradeCandidate> candidates = new ArrayList<>();
		List<AssetPosition> underweightPositions = positions.stream()
				.filter(position -> !isCash(position))
				.filter(position -> targetAllocation(position).compareTo(BigDecimal.ZERO) > 0)
				.filter(position -> targetAllocation(position).subtract(position.allocationPercent())
						.compareTo(REBALANCE_THRESHOLD_PERCENT) > 0)
				.sorted(Comparator.comparing(this::underweightPercent).reversed())
				.toList();

		for (AssetPosition position : underweightPositions) {
			if (remainingCash.compareTo(MIN_TRADE_AMOUNT) < 0) {
				break;
			}

			BigDecimal targetAllocation = targetAllocation(position);
			BigDecimal underweightPercent = targetAllocation.subtract(position.allocationPercent());
			BigDecimal deficitValue = allocationValue(totalValue, underweightPercent);
			BigDecimal estimatedAmount = roundTradeAmount(deficitValue.min(remainingCash).min(MAX_TRADE_AMOUNT));
			if (estimatedAmount.compareTo(MIN_TRADE_AMOUNT) < 0) {
				continue;
			}

			String rationale = String.format(
					Locale.US,
					"Deploy excess cash while current weight is %.2f%% against a %.2f%% target.",
					position.allocationPercent(),
					targetAllocation
			);
			candidates.add(new TradeCandidate(
					"Buy",
					position,
					estimatedAmount,
					withTechnicalContext(rationale, position),
					confidence(underweightPercent, position, "Buy"),
					underweightPercent
			));
			remainingCash = remainingCash.subtract(estimatedAmount);
		}

		return candidates;
	}

	private BigDecimal deployableCash(List<AssetPosition> positions, BigDecimal totalValue) {
		BigDecimal cashValue = positions.stream()
				.filter(this::isCash)
				.map(AssetPosition::marketValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal requiredReserve = allocationValue(totalValue, CASH_RESERVE_PERCENT);
		return cashValue.subtract(requiredReserve).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
	}

	private boolean isCash(AssetPosition position) {
		return "CASH".equalsIgnoreCase(position.symbol()) || "Cash".equalsIgnoreCase(position.assetClass());
	}

	private BigDecimal targetAllocation(AssetPosition position) {
		return TARGET_ALLOCATIONS.getOrDefault(position.symbol(), BigDecimal.ZERO);
	}

	private BigDecimal underweightPercent(AssetPosition position) {
		return targetAllocation(position).subtract(position.allocationPercent());
	}

	private static BigDecimal allocationValue(BigDecimal value, BigDecimal percent) {
		return value
				.multiply(percent, MONEY_CONTEXT)
				.divide(ONE_HUNDRED, MONEY_CONTEXT)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal roundTradeAmount(BigDecimal value) {
		return value
				.divide(TRADE_INCREMENT, 0, RoundingMode.DOWN)
				.multiply(TRADE_INCREMENT)
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static String confidence(BigDecimal deviationPercent, AssetPosition position, String action) {
		int score = baseConfidenceScore(deviationPercent);
		AssetIndicators indicators = position.indicators();
		if (indicators != null && indicators.available()) {
			score += technicalConfidenceAdjustment(indicators, action);
		}
		score = Math.max(1, Math.min(3, score));

		if (score == 3) {
			return "High";
		}
		if (score == 2) {
			return "Medium";
		}
		return "Low";
	}

	private static int baseConfidenceScore(BigDecimal deviationPercent) {
		if (deviationPercent.compareTo(new BigDecimal("10.00")) >= 0) {
			return 3;
		}
		if (deviationPercent.compareTo(new BigDecimal("4.00")) >= 0) {
			return 2;
		}
		return 1;
	}

	private static int technicalConfidenceAdjustment(AssetIndicators indicators, String action) {
		if ("Buy".equals(action)) {
			if (indicators.rsi14().compareTo(RSI_OVERBOUGHT) >= 0) {
				return -1;
			}
			if ("bullish".equals(indicators.trendClass())
					|| indicators.rsi14().compareTo(RSI_OVERSOLD) <= 0
					|| "bullish".equals(indicators.momentumClass())) {
				return 1;
			}
		}
		if ("Trim".equals(action)) {
			if (indicators.rsi14().compareTo(RSI_OVERSOLD) <= 0) {
				return -1;
			}
			if ("bearish".equals(indicators.trendClass())
					|| indicators.rsi14().compareTo(RSI_OVERBOUGHT) >= 0) {
				return 1;
			}
		}
		return 0;
	}

	private static String withTechnicalContext(String rationale, AssetPosition position) {
		AssetIndicators indicators = position.indicators();
		if (indicators == null || !indicators.available()) {
			return rationale;
		}
		return String.format(
				Locale.US,
				"%s Technicals show %s with %s momentum.",
				rationale,
				indicators.trend().toLowerCase(Locale.US),
				indicators.momentum().toLowerCase(Locale.US)
		);
	}

	private record TradeCandidate(
			String action,
			AssetPosition position,
			BigDecimal estimatedAmount,
			String rationale,
			String confidence,
			BigDecimal priority
	) {

		private RecommendedTrade toRecommendedTrade() {
			return new RecommendedTrade(
					action,
					position.symbol(),
					position.name(),
					estimatedAmount,
					rationale,
					confidence
			);
		}
	}
}
