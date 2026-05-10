package io.codepieces.tyche.recommendations.backtest;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.codepieces.tyche.analysis.TechnicalIndicatorService;
import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.assets.AssetPosition;
import io.codepieces.tyche.assets.PortfolioPosition;
import io.codepieces.tyche.assets.PortfolioPositionRepository;
import io.codepieces.tyche.market.MarketBar;
import io.codepieces.tyche.market.MarketDataService;
import io.codepieces.tyche.recommendations.model.RecommendationSignal;
import io.codepieces.tyche.recommendations.model.RecommendedTrade;
import io.codepieces.tyche.recommendations.scoring.TradeRecommendationService;
import org.springframework.stereotype.Service;

@Service
public class RecommendationBacktestService {

	private static final MathContext CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final int MIN_HISTORY = 200;
	private static final int HORIZON_DAYS = 20;
	private static final int STEP_DAYS = 20;

	private final PortfolioPositionRepository portfolioPositionRepository;
	private final MarketDataService marketDataService;
	private final TechnicalIndicatorService technicalIndicatorService;
	private final TradeRecommendationService recommendationService;

	public RecommendationBacktestService(
			PortfolioPositionRepository portfolioPositionRepository,
			MarketDataService marketDataService,
			TechnicalIndicatorService technicalIndicatorService,
			TradeRecommendationService recommendationService
	) {
		this.portfolioPositionRepository = portfolioPositionRepository;
		this.marketDataService = marketDataService;
		this.technicalIndicatorService = technicalIndicatorService;
		this.recommendationService = recommendationService;
	}

	public RecommendationBacktestResult runPortfolioBacktest() {
		List<PortfolioPosition> positions = portfolioPositionRepository.findCurrentPositions();
		Map<String, List<MarketBar>> histories = positions.stream()
				.filter(position -> !position.isCash())
				.collect(HashMap::new,
						(map, position) -> map.put(position.symbol(), marketDataService.dailyHistory(position.symbol(), position.marketPrice())),
						HashMap::putAll);
		int maxHistory = histories.values().stream()
				.mapToInt(List::size)
				.min()
				.orElse(0);
		List<PortfolioPosition> supportedPositions = positions.stream()
				.filter(position -> position.isCash() || histories.getOrDefault(position.symbol(), List.of()).size() == maxHistory)
				.toList();

		int evaluated = 0;
		int wins = 0;
		BigDecimal forwardReturnSum = BigDecimal.ZERO;
		BigDecimal signedReturnSum = BigDecimal.ZERO;

		for (int index = MIN_HISTORY; index + HORIZON_DAYS < maxHistory; index += STEP_DAYS) {
			int evaluationIndex = index;
			BigDecimal totalValue = supportedPositions.stream()
					.map(position -> historicalMarketValue(position, histories.get(position.symbol()), evaluationIndex))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			List<AssetPosition> assetPositions = supportedPositions.stream()
					.map(position -> assetPosition(position, histories.get(position.symbol()), evaluationIndex, totalValue))
					.toList();
			Map<String, RecommendationSignal> neutralSignals = supportedPositions.stream()
					.filter(position -> !position.isCash())
					.collect(HashMap::new,
							(map, position) -> map.put(position.symbol(), RecommendationSignal.neutral(position.symbol())),
							HashMap::putAll);

			List<RecommendedTrade> recommendations = recommendationService.recommendTrades(assetPositions, totalValue, neutralSignals);
			for (RecommendedTrade recommendation : recommendations) {
				if ("Hold".equals(recommendation.action())) {
					continue;
				}

				List<MarketBar> history = histories.get(recommendation.symbol());
				if (history == null) {
					continue;
				}
				MarketBar current = history.get(evaluationIndex - 1);
				MarketBar future = history.get(evaluationIndex + HORIZON_DAYS);
				BigDecimal forwardReturn = future.close().subtract(current.close())
						.divide(current.close(), 4, RoundingMode.HALF_UP);
				int direction = "Reduce".equals(recommendation.action()) ? -1 : 1;
				BigDecimal signedReturn = forwardReturn.multiply(BigDecimal.valueOf(direction), CONTEXT);

				evaluated++;
				if (signedReturn.compareTo(BigDecimal.ZERO) > 0) {
					wins++;
				}
				forwardReturnSum = forwardReturnSum.add(forwardReturn);
				signedReturnSum = signedReturnSum.add(signedReturn);
			}
		}

		if (evaluated == 0) {
			return new RecommendationBacktestResult(
					0,
					0,
					BigDecimal.ZERO.setScale(2),
					BigDecimal.ZERO.setScale(2),
					BigDecimal.ZERO.setScale(2),
					HORIZON_DAYS,
					TradeRecommendationService.MODEL_VERSION
			);
		}

		return new RecommendationBacktestResult(
				evaluated,
				wins,
				BigDecimal.valueOf(wins).divide(BigDecimal.valueOf(evaluated), 4, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP),
				forwardReturnSum.divide(BigDecimal.valueOf(evaluated), 4, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP),
				signedReturnSum.divide(BigDecimal.valueOf(evaluated), 4, RoundingMode.HALF_UP)
						.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP),
				HORIZON_DAYS,
				TradeRecommendationService.MODEL_VERSION
		);
	}

	private AssetPosition assetPosition(
			PortfolioPosition position,
			List<MarketBar> history,
			int index,
			BigDecimal totalValue
	) {
		if (position.isCash()) {
			return assetPosition(position, position.marketPrice(), AssetIndicators.unavailable(position.symbol()), totalValue);
		}
		BigDecimal marketPrice = history.get(index - 1).close();
		AssetIndicators indicators = technicalIndicatorService.indicatorsFor(position.symbol(), history.subList(0, index));
		return assetPosition(position, marketPrice, indicators, totalValue);
	}

	private static BigDecimal historicalMarketValue(PortfolioPosition position, List<MarketBar> history, int index) {
		if (position.isCash()) {
			return position.quantity().multiply(position.marketPrice(), CONTEXT).setScale(2, RoundingMode.HALF_UP);
		}
		if (history == null || history.size() <= index) {
			return BigDecimal.ZERO.setScale(2);
		}
		return position.quantity().multiply(history.get(index - 1).close(), CONTEXT).setScale(2, RoundingMode.HALF_UP);
	}

	private static AssetPosition assetPosition(
			PortfolioPosition position,
			BigDecimal marketPrice,
			AssetIndicators indicators,
			BigDecimal totalValue
	) {
		BigDecimal marketValue = position.quantity().multiply(marketPrice, CONTEXT).setScale(2, RoundingMode.HALF_UP);
		BigDecimal costBasis = position.quantity().multiply(position.averageCost(), CONTEXT);
		BigDecimal profitLoss = marketValue.subtract(costBasis);
		BigDecimal profitLossPercent = costBasis.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO.setScale(2)
				: profitLoss.divide(costBasis, CONTEXT).multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
		BigDecimal allocationPercent = totalValue.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO.setScale(2)
				: marketValue.divide(totalValue, CONTEXT).multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);
		return new AssetPosition(
				position.symbol(),
				position.name(),
				position.assetClass(),
				position.quantity(),
				position.averageCost(),
				marketPrice,
				marketValue,
				profitLoss.setScale(2, RoundingMode.HALF_UP),
				profitLossPercent,
				position.dayChangePercent(),
				allocationPercent,
				allocationPercent.max(BigDecimal.ONE).toPlainString() + "%",
				indicators
		);
	}
}
