package io.codepieces.tyche.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

import io.codepieces.tyche.assets.AssetIndicators;
import io.codepieces.tyche.market.MarketBar;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.averages.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

@Service
public class TechnicalIndicatorService {

	private static final int RSI_BARS = 14;
	private static final int SMA_SHORT_BARS = 50;
	private static final int SMA_LONG_BARS = 200;
	private static final int MACD_SHORT_BARS = 12;
	private static final int MACD_LONG_BARS = 26;
	private static final int MACD_SIGNAL_BARS = 9;

	public AssetIndicators indicatorsFor(String symbol, List<MarketBar> history) {
		if (history.size() < SMA_LONG_BARS) {
			return AssetIndicators.unavailable(symbol);
		}

		BarSeries series = toSeries(symbol, history);
		int index = series.getEndIndex();
		ClosePriceIndicator close = new ClosePriceIndicator(series);
		RSIIndicator rsi14 = new RSIIndicator(close, RSI_BARS);
		SMAIndicator sma50 = new SMAIndicator(close, SMA_SHORT_BARS);
		SMAIndicator sma200 = new SMAIndicator(close, SMA_LONG_BARS);
		MACDIndicator macd = new MACDIndicator(close, MACD_SHORT_BARS, MACD_LONG_BARS);
		Indicator<Num> macdSignal = macd.getSignalLine(MACD_SIGNAL_BARS);

		BigDecimal lastClose = value(close, index);
		BigDecimal rsi = value(rsi14, index);
		BigDecimal shortAverage = value(sma50, index);
		BigDecimal longAverage = value(sma200, index);
		BigDecimal macdValue = value(macd, index);
		BigDecimal signalValue = value(macdSignal, index);

		if (lastClose == null || rsi == null || shortAverage == null || longAverage == null
				|| macdValue == null || signalValue == null) {
			return AssetIndicators.unavailable(symbol);
		}

		return new AssetIndicators(
				symbol,
				rsi,
				shortAverage,
				longAverage,
				macdValue,
				signalValue,
				trend(lastClose, shortAverage, longAverage),
				trendClass(lastClose, shortAverage, longAverage),
				momentum(rsi, macdValue, signalValue),
				momentumClass(rsi, macdValue, signalValue)
		);
	}

	private static BarSeries toSeries(String symbol, List<MarketBar> history) {
		BarSeries series = new BaseBarSeriesBuilder()
				.withName(symbol)
				.build();

		for (MarketBar bar : history) {
			series.barBuilder()
					.timePeriod(Duration.ofDays(1))
					.endTime(bar.date().atStartOfDay().plusDays(1).toInstant(ZoneOffset.UTC))
					.openPrice(bar.open())
					.highPrice(bar.high())
					.lowPrice(bar.low())
					.closePrice(bar.close())
					.volume(bar.volume())
					.trades(1)
					.add();
		}

		return series;
	}

	private static BigDecimal value(Indicator<Num> indicator, int index) {
		Num value = indicator.getValue(index);
		if (Num.isNaNOrNull(value) || value.bigDecimalValue() == null) {
			return null;
		}
		return value.bigDecimalValue().setScale(2, RoundingMode.HALF_UP);
	}

	private static String trend(BigDecimal price, BigDecimal sma50, BigDecimal sma200) {
		if (price.compareTo(sma50) >= 0 && sma50.compareTo(sma200) >= 0) {
			return "Uptrend";
		}
		if (price.compareTo(sma50) < 0 && sma50.compareTo(sma200) < 0) {
			return "Downtrend";
		}
		return "Mixed";
	}

	private static String trendClass(BigDecimal price, BigDecimal sma50, BigDecimal sma200) {
		if (price.compareTo(sma50) >= 0 && sma50.compareTo(sma200) >= 0) {
			return "bullish";
		}
		if (price.compareTo(sma50) < 0 && sma50.compareTo(sma200) < 0) {
			return "bearish";
		}
		return "neutral";
	}

	private static String momentum(BigDecimal rsi, BigDecimal macd, BigDecimal signal) {
		if (rsi.compareTo(new BigDecimal("70")) >= 0) {
			return "Overbought";
		}
		if (rsi.compareTo(new BigDecimal("30")) <= 0) {
			return "Oversold";
		}
		if (macd.compareTo(signal) >= 0) {
			return "Positive";
		}
		return "Negative";
	}

	private static String momentumClass(BigDecimal rsi, BigDecimal macd, BigDecimal signal) {
		if (rsi.compareTo(new BigDecimal("70")) >= 0) {
			return "bearish";
		}
		if (rsi.compareTo(new BigDecimal("30")) <= 0) {
			return "bullish";
		}
		if (macd.compareTo(signal) >= 0) {
			return "bullish";
		}
		return "bearish";
	}
}
