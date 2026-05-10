package io.codepieces.tyche.assets;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;

@JdbcTest
@Import(JdbcPortfolioPositionRepository.class)
class JdbcPortfolioPositionRepositoryTests {

	@Autowired
	private JdbcPortfolioPositionRepository repository;

	@Test
	void readsCurrentPositionsFromPortfolioPositionsTable() {
		List<PortfolioPosition> positions = repository.findCurrentPositions();

		assertThat(positions).hasSize(5);
		assertThat(positions.getFirst().symbol()).isEqualTo("VOO");
		assertThat(positions.getFirst().quantity()).isEqualByComparingTo("18.0000");
		assertThat(positions.getLast().symbol()).isEqualTo("CASH");
	}
}
