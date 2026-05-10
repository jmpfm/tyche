package io.codepieces.tyche.recommendations.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.codepieces.tyche.recommendations.backtest.RecommendationBacktestResult;
import io.codepieces.tyche.recommendations.backtest.RecommendationBacktestService;
import io.codepieces.tyche.recommendations.model.RecommendationRunResult;
import io.codepieces.tyche.recommendations.workflow.TradeRecommendationWorkflow;

@RestController
@RequestMapping("/api/recommendations")
public class TradeRecommendationController {

	private final TradeRecommendationWorkflow workflow;
	private final RecommendationBacktestService backtestService;

	public TradeRecommendationController(
			TradeRecommendationWorkflow workflow,
			RecommendationBacktestService backtestService
	) {
		this.workflow = workflow;
		this.backtestService = backtestService;
	}

	@PostMapping("/generate")
	public RecommendationRunResult generate() {
		return workflow.generateAndPublish();
	}

	@GetMapping("/backtest")
	public RecommendationBacktestResult backtest() {
		return backtestService.runPortfolioBacktest();
	}
}
