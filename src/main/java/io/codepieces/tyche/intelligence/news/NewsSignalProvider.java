package io.codepieces.tyche.intelligence.news;

import io.codepieces.tyche.assets.PortfolioPosition;

public interface NewsSignalProvider {

	NewsSignal newsFor(PortfolioPosition position);
}
