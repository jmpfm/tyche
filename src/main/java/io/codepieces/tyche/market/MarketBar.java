package io.codepieces.tyche.market;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketBar(
		LocalDate date,
		BigDecimal open,
		BigDecimal high,
		BigDecimal low,
		BigDecimal close,
		BigDecimal volume
) {
}
