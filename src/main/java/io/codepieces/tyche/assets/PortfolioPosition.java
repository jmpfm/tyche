package io.codepieces.tyche.assets;

import java.math.BigDecimal;

public record PortfolioPosition(
		String symbol,
		String name,
		String assetClass,
		BigDecimal quantity,
		BigDecimal averageCost,
		BigDecimal marketPrice,
		BigDecimal dayChangePercent
) {

	public boolean isCash() {
		return "CASH".equalsIgnoreCase(symbol) || "Cash".equalsIgnoreCase(assetClass);
	}
}
