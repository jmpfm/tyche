package io.codepieces.tyche.recommendations.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tyche.recommendations")
public record RecommendationEngineProperties(
		Engine engine,
		Events events,
		State state
) {

	public record Engine(
			boolean enabled,
			Duration interval
	) {
	}

	public record Events(
			boolean enabled,
			String groupId,
			String technicalAnalysisTopic,
			String newsTopic
	) {
	}

	public record State(
			String technicalAnalysisTopic,
			String symbolNewsTopic,
			String globalNewsTopic,
			String changesTopic,
			Duration taTtl,
			Duration newsTtl
	) {
	}
}
