package io.codepieces.tyche.recommendations.model;

import java.math.BigDecimal;

public record FactorScores(
		BigDecimal technical,
		BigDecimal news,
		BigDecimal macro,
		BigDecimal fundamentals,
		BigDecimal portfolio,
		BigDecimal composite
) {
}
