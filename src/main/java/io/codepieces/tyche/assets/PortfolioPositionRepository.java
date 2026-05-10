package io.codepieces.tyche.assets;

import java.util.List;

public interface PortfolioPositionRepository {

	List<PortfolioPosition> findCurrentPositions();
}
