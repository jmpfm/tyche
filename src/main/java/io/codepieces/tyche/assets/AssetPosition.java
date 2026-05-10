package io.codepieces.tyche.assets;

import java.math.BigDecimal;

public record AssetPosition(
		String symbol,
		String name,
		String assetClass,
		BigDecimal quantity,
		BigDecimal averageCost,
		BigDecimal marketPrice,
		BigDecimal marketValue,
		BigDecimal unrealizedProfitLoss,
		BigDecimal unrealizedProfitLossPercent,
		BigDecimal dayChangePercent,
		BigDecimal allocationPercent,
		String allocationWidth,
		AssetIndicators indicators
) {
}
