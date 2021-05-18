import org.apache.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class Algorithms {
    private final static Logger LOGGER = Logger.getLogger(Algorithms.class);
    private final MarketUtil marketUtil;
    private final KlineDatapack klineData;

    public Algorithms(KlineDatapack klineData) {
        marketUtil = new MarketUtil();
        this.klineData = klineData;
    }

    public String rsiLT30(){
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;
            double[] rsiData = marketUtil.calculateRSIValues(klineData, 14);
            if (rsiData[rsiData.length - 1] < 30) {
                return ticker;
            }
        }
        return "";
    }

    public String fib618() {
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= klineData.get(klineData.size() - 1).getClose() - (60000 * 300))
                    .collect(Collectors.toList());
            double[] fibs = marketUtil.calculateKeyFibRetracements(cStickDataForFIB);
            double fib618 = fibs[2];
            double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
            double fibDiff = Math.abs(lastClose - fib618);
            double fibDiffPercent = fibDiff / lastClose;
            if (fibDiffPercent <= .02) {
                return ticker;
            }
        }
        return "";
    }

    public String rsiAndFib() {
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= klineData.get(klineData.size() - 1).getClose() - (60000 * 300))
                    .collect(Collectors.toList());

            double[] rsiData = marketUtil.calculateRSIValues(klineData, 14);
            if (rsiData[rsiData.length - 1] < 30) {
                double[] fibs = marketUtil.calculateKeyFibRetracements(cStickDataForFIB);
                double fib618 = fibs[2];
                double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
                double fibDiff = Math.abs(lastClose - fib618);
                double fibDiffPercent = fibDiff / lastClose;
                if (fibDiffPercent <= .005) {
                    return ticker;
                }
            }
        }
        return "";
    }

    public String lowRSIanyFib(int maxRSI, double fibRange, double volumeMin) {
        double best = 999;
        String bestTicker = "";
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= klineData.get(klineData.size() - 1).getClose() - (60000 * 300)) // 300 minutes ago
                    .collect(Collectors.toList());

            double[] rsiData = marketUtil.calculateRSIValues(klineData, 30);
            double volume = MarketUtil.getUSDVolumeAvg(klineData, 6);
            double rsi = (rsiData[rsiData.length - 1]);

            if (rsi < maxRSI && volume > volumeMin) {
                double[] fibs = marketUtil.calculateKeyFibRetracements(cStickDataForFIB);
                double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
                for (double d : fibs) {
                    double fibDiff = Math.abs(lastClose - d);
                    double fibDiffPercent = fibDiff / lastClose;
                    if (fibDiffPercent <= fibRange && rsi < best) {
                        bestTicker = ticker;
                        best = rsi;
                        break;
                    }
                }
            }
        }
        return bestTicker;
    }

    public String RSI_MACD_PER(int MACD_RSImax, int rsiRange, int maRangeSmall, int maRangeBig, int percentRange1, double percentMin1, int percentRange2, double percentMin2, int volumeRange, double volumeUSDMin) {
        double best = 999;
        String bestTicker = "";
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;

            double macd = marketUtil.calculateMACD(klineData, maRangeSmall, maRangeBig);
            double percent1 = marketUtil.getPercentChange(klineData, percentRange1);
            double percent2 = marketUtil.getPercentChange(klineData, percentRange2);
            double volume = MarketUtil.getUSDVolumeAvg(klineData, volumeRange);

            if (percent1 > percentMin1 && percent2 > percentMin2 && volume > volumeUSDMin) {
                double[] rsiData = marketUtil.calculateRSIValues(klineData, rsiRange);
                double MACD_RSI = (rsiData[rsiData.length - 1] + macd);
                if (MACD_RSI < MACD_RSImax && MACD_RSI < best) {
                    bestTicker = ticker;
                    best = MACD_RSI;
                }
            }
        }
        return bestTicker;
    }

}
