package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

import io.codepieces.tyche.analysis.TechnicalIndicatorService;
import io.codepieces.tyche.market.MarketDataService;
import org.springframework.stereotype.Service;

@Service
public class AssetPortfolioService {

	private static final MathContext MONEY_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final PortfolioPositionRepository portfolioPositionRepository;
	private final TradeRecommendationService tradeRecommendationService;
	private final MarketDataService marketDataService;
	private final TechnicalIndicatorService technicalIndicatorService;

	public AssetPortfolioService(
			PortfolioPositionRepository portfolioPositionRepository,
			TradeRecommendationService tradeRecommendationService,
			MarketDataService marketDataService,
			TechnicalIndicatorService technicalIndicatorService
	) {
		this.portfolioPositionRepository = portfolioPositionRepository;
		this.tradeRecommendationService = tradeRecommendationService;
		this.marketDataService = marketDataService;
		this.technicalIndicatorService = technicalIndicatorService;
	}

	public AssetPortfolio currentPortfolio() {
		List<PortfolioPosition> rawPositions = portfolioPositionRepository.findCurrentPositions();

		BigDecimal totalValue = rawPositions.stream()
				.map(AssetPortfolioService::marketValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCost = rawPositions.stream()
				.map(AssetPortfolioService::costBasis)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal unrealizedProfitLoss = totalValue.subtract(totalCost);
		BigDecimal dayChangeValue = rawPositions.stream()
				.map(AssetPortfolioService::dayChangeValue)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		List<AssetPosition> positions = rawPositions.stream()
				.map(position -> toAssetPosition(position, totalValue, indicatorsFor(position)))
				.toList();
		List<RecommendedTrade> recommendedTrades = tradeRecommendationService.recommendTrades(positions, totalValue);

		return new AssetPortfolio(
				money(totalValue),
				money(totalCost),
				money(unrealizedProfitLoss),
				percent(unrealizedProfitLoss, totalCost),
				money(dayChangeValue),
				percent(dayChangeValue, totalValue.subtract(dayChangeValue)),
				positions,
				recommendedTrades
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

	private AssetIndicators indicatorsFor(PortfolioPosition position) {
		if (position.isCash()) {
			return AssetIndicators.unavailable(position.symbol());
		}
		return technicalIndicatorService.indicatorsFor(
				position.symbol(),
				marketDataService.dailyHistory(position.symbol(), position.marketPrice())
		);
	}

	private static BigDecimal costBasis(PortfolioPosition position) {
		return position.quantity().multiply(position.averageCost(), MONEY_CONTEXT);
	}

	private static BigDecimal marketValue(PortfolioPosition position) {
		return position.quantity().multiply(position.marketPrice(), MONEY_CONTEXT);
	}

	private static BigDecimal dayChangeValue(PortfolioPosition position) {
		return marketValue(position)
				.multiply(position.dayChangePercent(), MONEY_CONTEXT)
				.divide(ONE_HUNDRED, MONEY_CONTEXT);
	}

	private static AssetPosition toAssetPosition(PortfolioPosition position, BigDecimal totalValue, AssetIndicators indicators) {
		BigDecimal marketValue = marketValue(position);
		BigDecimal costBasis = costBasis(position);
		BigDecimal unrealizedProfitLoss = marketValue.subtract(costBasis);
		BigDecimal allocationPercent = percent(marketValue, totalValue);

		return new AssetPosition(
				position.symbol(),
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
				indicators
		);
	}
}
