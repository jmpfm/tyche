package io.codepieces.tyche.assets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssetsController.class)
@Import({ AssetMoneyFormatter.class, AssetPortfolioService.class })
class AssetsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void redirectsHomeToAssetsView() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/assets"));
	}

	@Test
	void rendersAssetsViewWithPortfolioSnapshot() throws Exception {
		mockMvc.perform(get("/assets"))
				.andExpect(status().isOk())
				.andExpect(view().name("assets/index"))
				.andExpect(model().attributeExists("portfolio"))
				.andExpect(result -> {
					AssetPortfolio portfolio = (AssetPortfolio) result.getModelAndView().getModel().get("portfolio");
					assertThat(portfolio.positions(), hasSize(5));
					assertThat(portfolio.recommendedTrades(), hasSize(3));
				})
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Assets")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Recommended trades")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Execute now")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("$36,395.00")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Vanguard S&amp;P 500 ETF")));
	}
}
