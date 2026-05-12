package io.codepieces.tyche.recommendations.tracking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTrackedStockRepository implements TrackedStockRepository {

	private static final String SELECT_ALL = """
			select symbol, name, enabled, created_at, updated_at
			from tracked_stocks
			order by symbol
			""";
	private static final String SELECT_ENABLED = """
			select symbol, name, enabled, created_at, updated_at
			from tracked_stocks
			where enabled = true
			order by symbol
			""";
	private static final String SELECT_BY_SYMBOL = """
			select symbol, name, enabled, created_at, updated_at
			from tracked_stocks
			where symbol = ?
			""";
	private static final String UPSERT = """
			merge into tracked_stocks (symbol, name, enabled, created_at, updated_at)
			key(symbol) values (?, ?, ?, coalesce((select created_at from tracked_stocks where symbol = ?), current_timestamp()), current_timestamp())
			""";
	private static final String SET_ENABLED = """
			update tracked_stocks
			set enabled = ?, updated_at = current_timestamp()
			where symbol = ?
			""";
	private static final String DELETE = "delete from tracked_stocks where symbol = ?";

	private final JdbcTemplate jdbcTemplate;

	public JdbcTrackedStockRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<TrackedStock> findAll() {
		return jdbcTemplate.query(SELECT_ALL, JdbcTrackedStockRepository::map);
	}

	@Override
	public List<TrackedStock> findEnabled() {
		return jdbcTemplate.query(SELECT_ENABLED, JdbcTrackedStockRepository::map);
	}

	@Override
	public Optional<TrackedStock> findBySymbol(String symbol) {
		return jdbcTemplate.query(SELECT_BY_SYMBOL, JdbcTrackedStockRepository::map, symbol).stream().findFirst();
	}

	@Override
	public TrackedStock upsert(String symbol, String name, boolean enabled) {
		jdbcTemplate.update(UPSERT, symbol, name, enabled, symbol);
		return findBySymbol(symbol).orElseThrow();
	}

	@Override
	public boolean setEnabled(String symbol, boolean enabled) {
		return jdbcTemplate.update(SET_ENABLED, enabled, symbol) > 0;
	}

	@Override
	public boolean delete(String symbol) {
		return jdbcTemplate.update(DELETE, symbol) > 0;
	}

	private static TrackedStock map(ResultSet rs, int row) throws SQLException {
		return new TrackedStock(
				rs.getString("symbol"),
				rs.getString("name"),
				rs.getBoolean("enabled"),
				toInstant(rs.getTimestamp("created_at")),
				toInstant(rs.getTimestamp("updated_at"))
		);
	}

	private static Instant toInstant(Timestamp timestamp) {
		return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
	}
}
