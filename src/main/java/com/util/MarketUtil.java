package com.util;

import com.models.Candlestick;
import org.apache.log4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MarketUtil {
    private final static Logger LOGGER = Logger.getLogger(MarketUtil.class);

    public MarketUtil() {
    }


    private double calcSmmaUp(List<Candlestick> candlesticks, double n, int i, double avgUt1) {

        if (avgUt1 == 0) {
            double sumUpChanges = 0;

            for (int j = 0; j < n; j++) {
                double change = candlesticks.get(i - j).getClose() - candlesticks.get(i - j).getOpen();

                if (change > 0) {
                    sumUpChanges += change;
                }
            }
            return sumUpChanges / n;
        } else {
            double change = candlesticks.get(i).getClose() - candlesticks.get(i).getOpen();
            if (change < 0) {
                change = 0;
            }
            return ((avgUt1 * (n - 1)) + change) / n;
        }

    }

    private double calcSmmaDown(List<Candlestick> candlesticks, double n, int i, double avgDt1) {
        if (avgDt1 == 0) {
            double sumDownChanges = 0;

            for (int j = 0; j < n; j++) {
                double change = candlesticks.get(i - j).getClose() - candlesticks.get(i - j).getOpen();

                if (change < 0) {
                    sumDownChanges -= change;
                }
            }
            return sumDownChanges / n;
        } else {
            double change = candlesticks.get(i).getClose() - candlesticks.get(i).getOpen();
            if (change > 0) {
                change = 0;
            }
            return ((avgDt1 * (n - 1)) - change) / n;
        }
    }

    public double[] calculateRSIValues(List<Candlestick> candlesticks, int n) {
        double[] results = new double[candlesticks.size()];
        double ut1 = 0;
        double dt1 = 0;
        for (int i = 0; i < candlesticks.size(); i++) {
            if (i < (n)) {
                continue;
            }

            ut1 = calcSmmaUp(candlesticks, n, i, ut1);
            dt1 = calcSmmaDown(candlesticks, n, i, dt1);

            results[i] = 100.0 - 100.0 / (1.0 + (ut1 / dt1));
        }
        return results;
    }

    public double[] calculateKeyFibRetracements(List<Candlestick> candlesticks) {
        Candlestick min = candlesticks.stream().min(Comparator.comparing(Candlestick::getClose)).get();
        double max;
        Optional<Candlestick> maxOptional = candlesticks.subList(candlesticks.indexOf(min), candlesticks.size() - 1)
                .stream()
                .max(Comparator.comparing(Candlestick::getClose));

        if (maxOptional.isPresent() && (candlesticks.indexOf(maxOptional.get()) - candlesticks.indexOf(min) > 10)) {
            max = maxOptional.get().getClose();
        } else {
            max = 99999999;
        }

        double diff = max - min.getClose();

        double level382 = getIndFibRetracement(max, diff, .382);
        double level5 = getIndFibRetracement(max, diff, .5);
        double level618 = getIndFibRetracement(max, diff, .618);

        return new double[]{level382, level5, level618};
    }

    private double getIndFibRetracement(double max, double diff, double level) {
        return max - (diff * level);
    }

    /*Gets the percent change from [range] kline's ago closing price to the most recent kline closing price*/
    public double getPercentChange(List<Candlestick> candlesticks, int range) {
        Candlestick firstCandlestick = candlesticks.get(candlesticks.size() - (range + 1));
        double firstClose = firstCandlestick.getClose();
        Candlestick lastCandlestick = candlesticks.get(candlesticks.size() - 1);
        double lastClose = lastCandlestick.getClose();

        return ((100 / firstClose) * lastClose);
    }

    /*Gets the current moving average over the most recent [range] klines*/
    public double calculateMA(List<Candlestick> candlesticks, int range) {
        double MA = 0;
        for (int i = 0; i < candlesticks.size() - 1; i++) {
            if (i >= candlesticks.size() - 1 - range) {
                MA += candlesticks.get(i).getClose();
            }
        }
        MA = MA / range;
        return MA;
    }

    /*Gets the percent difference between two different ranged moving averages*/
    public double calculateMACD(List<Candlestick> candlesticks, int smallRange, int bigRange) {
        return (((calculateMA(candlesticks, smallRange)) / (calculateMA(candlesticks, bigRange))) * 100);
    }

    /*Calculates the total volume over the last [range] klines*/
    public static double getUSDVolumeAvg(List<Candlestick> candlesticks, int range) {
        double Vol = 0;
        for (int i = 0; i < candlesticks.size() - 1; i++) {
            if (i >= candlesticks.size() - 1 - range) {
                Vol += candlesticks.get(i).getVolume();
            }
        }
        return Vol / range;
    }
}
