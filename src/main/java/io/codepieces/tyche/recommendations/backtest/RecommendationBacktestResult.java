package io.codepieces.tyche.recommendations.backtest;

import java.math.BigDecimal;

public record RecommendationBacktestResult(
		int evaluatedSignals,
		int winningSignals,
		BigDecimal hitRate,
		BigDecimal averageForwardReturn,
		BigDecimal averageSignedReturn,
		int horizonTradingDays,
		String modelVersion
) {
}
