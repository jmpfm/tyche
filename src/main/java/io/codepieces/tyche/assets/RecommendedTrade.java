package io.codepieces.tyche.assets;

import java.math.BigDecimal;

public record RecommendedTrade(
		String action,
		String symbol,
		String name,
		BigDecimal estimatedAmount,
		String rationale,
		String confidence
) {
}
