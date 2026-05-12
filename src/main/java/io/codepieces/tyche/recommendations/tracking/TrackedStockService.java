package io.codepieces.tyche.recommendations.tracking;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class TrackedStockService {

	private final TrackedStockRepository repository;

	public TrackedStockService(TrackedStockRepository repository) {
		this.repository = repository;
	}

	public List<TrackedStock> allTrackedStocks() {
		return repository.findAll();
	}

	public Set<String> enabledSymbols() {
		return repository.findEnabled().stream().map(TrackedStock::symbol).collect(java.util.stream.Collectors.toSet());
	}

	public TrackedStock trackedStock(String symbol) {
		String normalized = normalizeSymbol(symbol);
		return repository.findBySymbol(normalized)
				.orElseThrow(() -> new TrackedStockNotFoundException(normalized));
	}

	public TrackedStock create(TrackedStockRequest request) {
		String symbol = normalizeSymbol(request.symbol());
		String name = normalizeName(request.name());
		boolean enabled = request.enabled() == null || request.enabled();
		return repository.upsert(symbol, name, enabled);
	}

	public TrackedStock update(String symbol, TrackedStockRequest request) {
		String normalized = normalizeSymbol(symbol);
		TrackedStock existing = trackedStock(normalized);
		String name = request.name() == null ? existing.name() : normalizeName(request.name());
		boolean enabled = request.enabled() == null ? existing.enabled() : request.enabled();
		return repository.upsert(normalized, name, enabled);
	}

	public TrackedStock updateEnabled(String symbol, boolean enabled) {
		String normalized = normalizeSymbol(symbol);
		if (!repository.setEnabled(normalized, enabled)) {
			throw new TrackedStockNotFoundException(normalized);
		}
		return trackedStock(normalized);
	}

	public void delete(String symbol) {
		String normalized = normalizeSymbol(symbol);
		if (!repository.delete(normalized)) {
			throw new TrackedStockNotFoundException(normalized);
		}
	}

	public boolean isEnabledTrackedSymbol(String symbol) {
		if (symbol == null) {
			return false;
		}
		return repository.findBySymbol(normalizeSymbol(symbol))
				.map(TrackedStock::enabled)
				.orElse(false);
	}

	private static String normalizeSymbol(String symbol) {
		if (symbol == null || symbol.isBlank()) {
			throw new InvalidTrackedStockException("symbol must not be blank");
		}
		return symbol.trim().toUpperCase(Locale.US);
	}

	private static String normalizeName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidTrackedStockException("name must not be blank");
		}
		return name.trim();
	}
}
