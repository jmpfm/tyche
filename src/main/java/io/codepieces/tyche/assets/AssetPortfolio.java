package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.util.List;

public record AssetPortfolio(
		BigDecimal totalValue,
		BigDecimal totalCost,
		BigDecimal unrealizedProfitLoss,
		BigDecimal unrealizedProfitLossPercent,
		BigDecimal dayChangeValue,
		BigDecimal dayChangePercent,
		List<AssetPosition> positions,
		List<RecommendedTrade> recommendedTrades
) {
}
