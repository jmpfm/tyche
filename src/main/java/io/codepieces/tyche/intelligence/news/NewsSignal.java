package io.codepieces.tyche.intelligence.news;

import java.math.BigDecimal;
import java.util.List;

public record NewsSignal(
		BigDecimal score,
		List<String> events,
		String source,
		boolean available
) {

	public static NewsSignal neutral(String source) {
		return new NewsSignal(BigDecimal.ZERO.setScale(2), List.of(), source, false);
	}
}
