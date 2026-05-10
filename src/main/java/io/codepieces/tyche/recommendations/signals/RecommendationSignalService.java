package io.codepieces.tyche.recommendations.signals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.codepieces.tyche.assets.PortfolioPosition;
import io.codepieces.tyche.intelligence.fundamentals.FinancialStatementSignal;
import io.codepieces.tyche.intelligence.fundamentals.FinancialStatementSignalProvider;
import io.codepieces.tyche.intelligence.macro.MacroSignal;
import io.codepieces.tyche.intelligence.macro.MacroSignalProvider;
import io.codepieces.tyche.intelligence.news.NewsSignal;
import io.codepieces.tyche.intelligence.news.NewsSignalProvider;
import io.codepieces.tyche.recommendations.model.RecommendationSignal;
import org.springframework.stereotype.Service;

@Service
public class RecommendationSignalService {

	private final NewsSignalProvider newsSignalProvider;
	private final MacroSignalProvider macroSignalProvider;
	private final FinancialStatementSignalProvider financialStatementSignalProvider;

	public RecommendationSignalService(
			NewsSignalProvider newsSignalProvider,
			MacroSignalProvider macroSignalProvider,
			FinancialStatementSignalProvider financialStatementSignalProvider
	) {
		this.newsSignalProvider = newsSignalProvider;
		this.macroSignalProvider = macroSignalProvider;
		this.financialStatementSignalProvider = financialStatementSignalProvider;
	}

	public Map<String, RecommendationSignal> signalsFor(List<PortfolioPosition> positions) {
		MacroSignal macro = macroSignalProvider.currentMacroSignal();
		Map<String, RecommendationSignal> signals = new HashMap<>();

		for (PortfolioPosition position : positions) {
			if (position.isCash()) {
				continue;
			}

			NewsSignal news = newsSignalProvider.newsFor(position);
			FinancialStatementSignal fundamentals = financialStatementSignalProvider.fundamentalsFor(position);
			List<String> events = new ArrayList<>();
			events.addAll(news.events());
			events.addAll(macro.events());
			events.addAll(fundamentals.events());

			List<String> sources = new ArrayList<>();
			sources.add(news.source());
			sources.add(macro.source());
			sources.add(fundamentals.source());

			signals.put(position.symbol(), new RecommendationSignal(
					position.symbol(),
					news.score(),
					macro.score(),
					fundamentals.score(),
					events.stream().limit(6).toList(),
					sources,
					news.available() || macro.available() || fundamentals.available()
			));
		}

		return signals;
	}
}
