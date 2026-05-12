package io.codepieces.tyche.recommendations.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class TrackedStockServiceTests {

	@Test
	void createsStockWithUppercaseSymbol() {
		InMemoryTrackedStockRepository repository = new InMemoryTrackedStockRepository();
		TrackedStockService service = new TrackedStockService(repository);

		TrackedStock created = service.create(new TrackedStockRequest("aapl", "Apple Inc.", true));

		assertThat(created.symbol()).isEqualTo("AAPL");
		assertThat(service.isEnabledTrackedSymbol("AAPL")).isTrue();
	}

	@Test
	void rejectsBlankSymbol() {
		InMemoryTrackedStockRepository repository = new InMemoryTrackedStockRepository();
		TrackedStockService service = new TrackedStockService(repository);

		assertThatThrownBy(() -> service.create(new TrackedStockRequest(" ", "Apple Inc.", true)))
				.isInstanceOf(InvalidTrackedStockException.class);
	}

	@Test
	void updatesEnabledState() {
		InMemoryTrackedStockRepository repository = new InMemoryTrackedStockRepository();
		TrackedStockService service = new TrackedStockService(repository);
		service.create(new TrackedStockRequest("MSFT", "Microsoft", true));

		TrackedStock updated = service.updateEnabled("MSFT", false);

		assertThat(updated.enabled()).isFalse();
		assertThat(service.isEnabledTrackedSymbol("MSFT")).isFalse();
	}

	private static final class InMemoryTrackedStockRepository implements TrackedStockRepository {

		private final Map<String, TrackedStock> store = new LinkedHashMap<>();

		@Override
		public List<TrackedStock> findAll() {
			return new ArrayList<>(store.values());
		}

		@Override
		public List<TrackedStock> findEnabled() {
			return store.values().stream().filter(TrackedStock::enabled).toList();
		}

		@Override
		public Optional<TrackedStock> findBySymbol(String symbol) {
			return Optional.ofNullable(store.get(symbol));
		}

		@Override
		public TrackedStock upsert(String symbol, String name, boolean enabled) {
			TrackedStock stock = new TrackedStock(
					symbol,
					name,
					enabled,
					store.containsKey(symbol) ? store.get(symbol).createdAt() : Instant.now(),
					Instant.now()
			);
			store.put(symbol, stock);
			return stock;
		}

		@Override
		public boolean setEnabled(String symbol, boolean enabled) {
			TrackedStock current = store.get(symbol);
			if (current == null) {
				return false;
			}
			store.put(symbol, new TrackedStock(symbol, current.name(), enabled, current.createdAt(), Instant.now()));
			return true;
		}

		@Override
		public boolean delete(String symbol) {
			return store.remove(symbol) != null;
		}
	}
}
