package io.codepieces.tyche.assets;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class AssetMoneyFormatter {

	public String format(BigDecimal value) {
		NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
		return formatter.format(value);
	}
}
