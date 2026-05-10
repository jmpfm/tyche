package io.codepieces.tyche.intelligence.fundamentals;

import java.math.BigDecimal;
import java.util.List;

public record FinancialStatementSignal(
		BigDecimal score,
		List<String> events,
		String source,
		boolean available
) {

	public static FinancialStatementSignal neutral(String source) {
		return new FinancialStatementSignal(BigDecimal.ZERO.setScale(2), List.of(), source, false);
	}
}
