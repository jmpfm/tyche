package io.codepieces.tyche.recommendations.tracking;

public class TrackedStockNotFoundException extends RuntimeException {

	public TrackedStockNotFoundException(String symbol) {
		super("Tracked stock not found: " + symbol);
	}
}
