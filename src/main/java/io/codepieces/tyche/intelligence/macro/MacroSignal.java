package io.codepieces.tyche.intelligence.macro;

import java.math.BigDecimal;
import java.util.List;

public record MacroSignal(
		BigDecimal score,
		List<String> events,
		String source,
		boolean available
) {

	public static MacroSignal neutral(String source) {
		return new MacroSignal(BigDecimal.ZERO.setScale(2), List.of(), source, false);
	}
}
