package io.codepieces.tyche.market;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

	private static final int HISTORY_LENGTH = 240;
	private static final MathContext PRICE_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);
	private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 5, 9);
	private static final Map<String, MarketProfile> PROFILES = Map.of(
			"VOO", new MarketProfile(360.00, 0.00085, 0.0080, 0.0100, 0.0900, 4_850_000),
			"AAPL", new MarketProfile(145.00, 0.00065, 0.0105, 0.0160, -0.0400, 59_000_000),
			"MSFT", new MarketProfile(335.00, 0.00090, 0.0090, 0.0140, 0.1200, 24_000_000),
			"BTC", new MarketProfile(41_500.00, 0.00175, 0.0200, 0.0320, 0.2200, 34_000)
	);

	public List<MarketBar> dailyHistory(String symbol, BigDecimal latestClose) {
		MarketProfile profile = PROFILES.get(symbol.toUpperCase(Locale.US));
		if (profile == null || latestClose == null || latestClose.compareTo(BigDecimal.ZERO) <= 0) {
			return List.of();
		}

		List<BigDecimal> rawCloses = rawCloses(profile);
		BigDecimal scale = latestClose.divide(rawCloses.getLast(), PRICE_CONTEXT);
		List<MarketBar> bars = new ArrayList<>(HISTORY_LENGTH);
		BigDecimal previousClose = null;

		for (int index = 0; index < HISTORY_LENGTH; index++) {
			BigDecimal close = money(rawCloses.get(index).multiply(scale, PRICE_CONTEXT));
			if (index == HISTORY_LENGTH - 1) {
				close = money(latestClose);
			}

			BigDecimal open = previousClose == null ? close : previousClose;
			BigDecimal rangeFactor = BigDecimal.valueOf(profile.volatility() * (1.00 + Math.abs(Math.sin(index / 9.0)) * 0.45));
			BigDecimal high = money(open.max(close).multiply(BigDecimal.ONE.add(rangeFactor), PRICE_CONTEXT));
			BigDecimal low = money(open.min(close).multiply(BigDecimal.ONE.subtract(rangeFactor), PRICE_CONTEXT));
			BigDecimal volume = BigDecimal.valueOf(profile.baseVolume() * (1.00 + Math.abs(Math.sin(index / 11.0)) * 0.20))
					.setScale(0, RoundingMode.HALF_UP);

			bars.add(new MarketBar(SNAPSHOT_DATE.minusDays(HISTORY_LENGTH - index - 1L), open, high, low, close, volume));
			previousClose = close;
		}

		return bars;
	}

	private static List<BigDecimal> rawCloses(MarketProfile profile) {
		List<BigDecimal> closes = new ArrayList<>(HISTORY_LENGTH);

		for (int index = 0; index < HISTORY_LENGTH; index++) {
			double trend = profile.startPrice() * Math.pow(1.00 + profile.dailyDrift(), index);
			double cycle = 1.00
					+ Math.sin(index / 13.0) * profile.cycleAmplitude()
					+ Math.cos(index / 29.0) * profile.cycleAmplitude() * 0.45;
			double recentWeight = Math.max(0.00, (index - (HISTORY_LENGTH - 35.0)) / 35.0);
			double recentShift = 1.00 + recentWeight * profile.recentImpulse();
			closes.add(BigDecimal.valueOf(trend * cycle * recentShift));
		}

		return closes;
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private record MarketProfile(
			double startPrice,
			double dailyDrift,
			double cycleAmplitude,
			double volatility,
			double recentImpulse,
			long baseVolume
	) {
	}
}
