import org.apache.log4j.Logger;
import util.GeneralUtil;

import java.util.List;
import java.util.stream.Collectors;

public class Algorithms {
    private final static Logger LOGGER = Logger.getLogger(DiscordBot.class);
    private final MarketUtil marketUtil;
    private KlineDatapack klineData;

    public Algorithms(KlineDatapack klineData) {
        marketUtil = new MarketUtil();
        this.klineData = klineData;
    }

    public String rsiLT30() {
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = this.klineData.getKline4hData().get(ticker);
            if (klineData.isEmpty())
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
            List<Candlestick> klineData = this.klineData.getKline4hData().get(ticker);
            if (klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= GeneralUtil.getDateDeltaUnix(-20))
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
            List<Candlestick> klineData = this.klineData.getKline4hData().get(ticker);
            if (klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= GeneralUtil.getDateDeltaUnix(-20))
                    .collect(Collectors.toList());

            double[] rsiData = marketUtil.calculateRSIValues(klineData, 14);
            if (rsiData[rsiData.length - 1] < 30) {
                double[] fibs = marketUtil.calculateKeyFibRetracements(cStickDataForFIB);
                double fib618 = fibs[2];
                double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
                double fibDiff = Math.abs(lastClose - fib618);
                double fibDiffPercent = fibDiff / lastClose;
                if (fibDiffPercent <= .02) {
                    return ticker;
                }
            }
        }
        return "";
    }
}
