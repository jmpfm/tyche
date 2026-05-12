package io.codepieces.tyche.recommendations.engine;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.assets.AssetPosition;
import io.codepieces.tyche.assets.PortfolioPosition;
import io.codepieces.tyche.assets.PortfolioPositionRepository;
import io.codepieces.tyche.recommendations.delivery.TradeRecommendationPublisher;
import io.codepieces.tyche.recommendations.events.NewsEvent;
import io.codepieces.tyche.recommendations.events.TechnicalAnalysisEvent;
import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import io.codepieces.tyche.recommendations.model.RecommendationSignal;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import io.codepieces.tyche.recommendations.scoring.TradeRecommendationService;
import io.codepieces.tyche.recommendations.state.GlobalRecommendationState;
import io.codepieces.tyche.recommendations.state.RecommendationStateStore;
import io.codepieces.tyche.recommendations.state.SymbolRecommendationState;
import io.codepieces.tyche.recommendations.tracking.TrackedStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TradingRecommendationEngine {

	private static final MathContext MONEY_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final PortfolioPositionRepository portfolioPositionRepository;
	private final TradeRecommendationService tradeRecommendationService;
	private final RecommendationStateStore recommendationStateStore;
	private final TrackedStockService trackedStockService;
	private final TradeRecommendationPublisher publisher;
	private final String kafkaTopic;
	private Instant lastEvaluationAt = Instant.EPOCH;

	public TradingRecommendationEngine(
			PortfolioPositionRepository portfolioPositionRepository,
			TradeRecommendationService tradeRecommendationService,
			RecommendationStateStore recommendationStateStore,
			TrackedStockService trackedStockService,
			TradeRecommendationPublisher publisher,
			@Value("${tyche.recommendations.kafka.topic:tyche.trade-recommendations.v1}") String kafkaTopic
	) {
		this.portfolioPositionRepository = portfolioPositionRepository;
		this.tradeRecommendationService = tradeRecommendationService;
		this.recommendationStateStore = recommendationStateStore;
		this.trackedStockService = trackedStockService;
		this.publisher = publisher;
		this.kafkaTopic = kafkaTopic;
	}

	public synchronized RecommendationRunResult evaluateChangedSymbols() {
		Instant startedAt = Instant.now();
		Set<String> trackedSymbols = trackedStockService.enabledSymbols();
		Set<String> changedSymbols = recommendationStateStore.drainChangedSymbolsSince(lastEvaluationAt);
		List<String> symbolsToEvaluate = changedSymbols.stream()
				.filter(trackedSymbols::contains)
				.sorted()
				.toList();

		List<PortfolioPosition> rawPositions = portfolioPositionRepository.findCurrentPositions();
		BigDecimal totalValue = totalValue(rawPositions);
		List<AssetPosition> positions = toAssetPositions(rawPositions, totalValue);
		BigDecimal deployableCash = tradeRecommendationService.deployableCashFor(positions, totalValue);

		List<RecommendedTrade> recommendations = new ArrayList<>();
		GlobalRecommendationState globalState = recommendationStateStore.globalState();
		for (String symbol : symbolsToEvaluate) {
			Optional<AssetPosition> maybePosition = positions.stream()
					.filter(position -> position.symbol().equalsIgnoreCase(symbol))
					.findFirst();
			if (maybePosition.isEmpty()) {
				continue;
			}
			Optional<SymbolRecommendationState> maybeState = recommendationStateStore.stateFor(symbol);
			if (maybeState.isEmpty() || maybeState.get().technicalAnalysis() == null) {
				continue;
			}
			AssetPosition basePosition = maybePosition.get();
			AssetPosition withTa = withIndicators(basePosition, maybeState.get().technicalAnalysis());
			RecommendationSignal signal = signalFor(symbol, maybeState.get(), globalState);
			tradeRecommendationService.recommendTradeForSymbol(withTa, totalValue, deployableCash, signal)
					.ifPresent(recommendations::add);
		}

		recommendations.sort(Comparator.comparing(RecommendedTrade::symbol));
		publisher.publish(recommendations);
		lastEvaluationAt = startedAt;
		return new RecommendationRunResult(startedAt, recommendations.size(), kafkaTopic, recommendations);
	}

	public RecommendationRunResult evaluateNow() {
		Instant previous = lastEvaluationAt;
		lastEvaluationAt = Instant.EPOCH;
		RecommendationRunResult result = evaluateChangedSymbols();
		lastEvaluationAt = previous.isAfter(result.generatedAt()) ? previous : result.generatedAt();
		return result;
	}

	private static AssetPosition withIndicators(AssetPosition position, TechnicalAnalysisEvent ta) {
		AssetIndicators indicators = new AssetIndicators(
				position.symbol(),
				ta.rsi14(),
				ta.sma50(),
				ta.sma200(),
				ta.macd(),
				ta.macdSignal(),
				ta.trendClass(),
				ta.trendClass(),
				ta.momentumClass(),
				ta.momentumClass()
		);
		return new AssetPosition(
				position.symbol(),
				position.name(),
				position.assetClass(),
				position.quantity(),
				position.averageCost(),
				position.marketPrice(),
				position.marketValue(),
				position.unrealizedProfitLoss(),
				position.unrealizedProfitLossPercent(),
				position.dayChangePercent(),
				position.allocationPercent(),
				position.allocationWidth(),
				indicators
		);
	}

	private static RecommendationSignal signalFor(
			String symbol,
			SymbolRecommendationState symbolState,
			GlobalRecommendationState globalState
	) {
		BigDecimal newsScore = averageScore(symbolState.symbolNews());
		BigDecimal macroScore = averageScore(globalState.news());
		List<String> events = new ArrayList<>();
		symbolState.symbolNews().stream().map(NewsEvent::headline).filter(h -> h != null && !h.isBlank()).limit(3).forEach(events::add);
		globalState.news().stream().map(NewsEvent::headline).filter(h -> h != null && !h.isBlank()).limit(2).forEach(events::add);
		List<String> sources = new ArrayList<>();
		symbolState.symbolNews().stream().map(NewsEvent::source).filter(s -> s != null && !s.isBlank()).distinct().limit(3).forEach(sources::add);
		globalState.news().stream().map(NewsEvent::source).filter(s -> s != null && !s.isBlank()).distinct().limit(2).forEach(sources::add);
		return new RecommendationSignal(symbol, clamp(newsScore), clamp(macroScore), BigDecimal.ZERO.setScale(2), events, sources, !events.isEmpty());
	}

	private static BigDecimal averageScore(List<NewsEvent> events) {
		if (events.isEmpty()) {
			return BigDecimal.ZERO.setScale(2);
		}
		BigDecimal sum = events.stream()
				.map(NewsEvent::score)
				.filter(score -> score != null)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return sum.divide(BigDecimal.valueOf(events.size()), 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal clamp(BigDecimal value) {
		BigDecimal min = new BigDecimal("-1.00");
		BigDecimal max = new BigDecimal("1.00");
		if (value.compareTo(min) < 0) {
			return min;
		}
		if (value.compareTo(max) > 0) {
			return max;
		}
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal totalValue(List<PortfolioPosition> positions) {
		return positions.stream().map(TradingRecommendationEngine::marketValue).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static List<AssetPosition> toAssetPositions(List<PortfolioPosition> rawPositions, BigDecimal totalValue) {
		return rawPositions.stream()
				.map(position -> toAssetPosition(position, totalValue))
				.toList();
	}

	private static AssetPosition toAssetPosition(PortfolioPosition position, BigDecimal totalValue) {
		BigDecimal marketValue = marketValue(position);
		BigDecimal costBasis = costBasis(position);
		BigDecimal unrealizedProfitLoss = marketValue.subtract(costBasis);
		BigDecimal allocationPercent = percent(marketValue, totalValue);

		return new AssetPosition(
				position.symbol().toUpperCase(Locale.US),
				position.name(),
				position.assetClass(),
				position.quantity(),
				money(position.averageCost()),
				money(position.marketPrice()),
				money(marketValue),
				money(unrealizedProfitLoss),
				percent(unrealizedProfitLoss, costBasis),
				position.dayChangePercent().setScale(2, RoundingMode.HALF_UP),
				allocationPercent,
				allocationPercent.max(BigDecimal.ONE).toPlainString() + "%",
				AssetIndicators.unavailable(position.symbol())
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

	private static BigDecimal costBasis(PortfolioPosition position) {
		return position.quantity().multiply(position.averageCost(), MONEY_CONTEXT);
	}

	private static BigDecimal marketValue(PortfolioPosition position) {
		return position.quantity().multiply(position.marketPrice(), MONEY_CONTEXT);
	}
}
