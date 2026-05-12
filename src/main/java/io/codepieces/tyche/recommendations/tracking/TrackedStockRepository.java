package io.codepieces.tyche.recommendations.tracking;

import java.util.List;
import java.util.Optional;

public interface TrackedStockRepository {

	List<TrackedStock> findAll();

	List<TrackedStock> findEnabled();

	Optional<TrackedStock> findBySymbol(String symbol);

	TrackedStock upsert(String symbol, String name, boolean enabled);

	boolean setEnabled(String symbol, boolean enabled);

	boolean delete(String symbol);
}
