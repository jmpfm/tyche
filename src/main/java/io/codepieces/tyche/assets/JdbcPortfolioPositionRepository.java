package io.codepieces.tyche.assets;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPortfolioPositionRepository implements PortfolioPositionRepository {

	private static final String SELECT_CURRENT_POSITIONS = """
			select symbol, name, asset_class, quantity, average_cost, market_price, day_change_percent
			from portfolio_positions
			order by display_order, symbol
			""";

	private final JdbcTemplate jdbcTemplate;

	public JdbcPortfolioPositionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<PortfolioPosition> findCurrentPositions() {
		return jdbcTemplate.query(SELECT_CURRENT_POSITIONS, JdbcPortfolioPositionRepository::mapPosition);
	}

	private static PortfolioPosition mapPosition(ResultSet resultSet, int rowNumber) throws SQLException {
		return new PortfolioPosition(
				resultSet.getString("symbol"),
				resultSet.getString("name"),
				resultSet.getString("asset_class"),
				resultSet.getBigDecimal("quantity"),
				resultSet.getBigDecimal("average_cost"),
				resultSet.getBigDecimal("market_price"),
				resultSet.getBigDecimal("day_change_percent")
		);
	}
}
