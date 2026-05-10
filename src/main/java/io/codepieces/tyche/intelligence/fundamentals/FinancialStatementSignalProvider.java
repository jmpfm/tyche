package io.codepieces.tyche.intelligence.fundamentals;

import io.codepieces.tyche.assets.PortfolioPosition;

public interface FinancialStatementSignalProvider {

	FinancialStatementSignal fundamentalsFor(PortfolioPosition position);
}
