package io.codepieces.tyche.assets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AssetsController {

	private final AssetPortfolioService assetPortfolioService;
	private final AssetMoneyFormatter assetMoneyFormatter;

	public AssetsController(AssetPortfolioService assetPortfolioService, AssetMoneyFormatter assetMoneyFormatter) {
		this.assetPortfolioService = assetPortfolioService;
		this.assetMoneyFormatter = assetMoneyFormatter;
	}

	@GetMapping("/")
	public String home() {
		return "redirect:/assets";
	}

	@GetMapping("/assets")
	public String assets(Model model) {
		model.addAttribute("portfolio", assetPortfolioService.currentPortfolio());
		model.addAttribute("money", assetMoneyFormatter);
		return "assets/index";
	}
}
