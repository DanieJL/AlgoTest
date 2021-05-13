import org.apache.log4j.Logger;
import org.json.JSONArray;
import util.ApiClient;
import util.GeneralUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Algorithms {
    private final static Logger LOGGER = Logger.getLogger(DiscordBot.class);
    private static String[] allowedTickers;

    static {
        /*Load list of allowed coins into array*/
        try {
            BufferedReader br = new BufferedReader(new FileReader("allowed.txt"));
            String str;
            List<String> list = new ArrayList<>();
            while ((str = br.readLine()) != null) {
                list.add(str);
            }
            allowedTickers = list.toArray(new String[0]);
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String rsiGt60() {
        for (String ticker : allowedTickers) {
            List<Candlestick> cStickDataForRSI = getKlineData(ticker, "4h");
            if (cStickDataForRSI.isEmpty())
                continue;
            double[] rsiData = MarketUtil.calculateRSIValues(cStickDataForRSI, 14);
            if (rsiData[rsiData.length - 1] < 30) {
                return ticker;
            }
        }
        return "";
    }

    public String fib618() {
        for (String ticker : allowedTickers) {
            List<Candlestick> klineData = getKlineData(ticker, "4h");
            if (klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= GeneralUtil.getDateDeltaUnix(-20))
                    .collect(Collectors.toList());
            double[] fibs = MarketUtil.calculateKeyFibRetracements(cStickDataForFIB);
            double fib618 = fibs[2];
            double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
            double fibDiff = Math.abs(lastClose - fib618);
            double fibDiffPercent = fibDiff / lastClose;

            if (fibDiffPercent <= .02) {
                LOGGER.info(".618 FIB: " + fib618 + "\nLast Close: " + lastClose);
                return ticker;
            }
        }
        return "";
    }

    public String rsiAndFib() {
        for (String ticker : allowedTickers) {
            List<Candlestick> klineData = getKlineData(ticker, "4h");
            if (klineData.isEmpty())
                continue;
            List<Candlestick> cStickDataForFIB = klineData
                    .stream()
                    .filter(stick -> stick.getOpenTime() >= GeneralUtil.getDateDeltaUnix(-20))
                    .collect(Collectors.toList());

            double[] rsiData = MarketUtil.calculateRSIValues(klineData, 14);
            if (rsiData[rsiData.length - 1] < 30) {
                double[] fibs = MarketUtil.calculateKeyFibRetracements(cStickDataForFIB);
                double fib618 = fibs[2];
                double lastClose = cStickDataForFIB.get(cStickDataForFIB.size() - 1).getClose();
                double fibDiff = Math.abs(lastClose - fib618);
                double fibDiffPercent = fibDiff / lastClose;

                if (fibDiffPercent <= .02) {
                    LOGGER.info(".618 FIB: " + fib618 + "\nLast Close: " + lastClose);
                    return ticker;
                }
            }
        }
        return "";
    }

    public List<Candlestick> getKlineData(String symbol, String interval) {
        List<Candlestick> allCandlesticks = new ArrayList<>();

        String startTime = "1604221113";
        String url = "https://api.binance.us/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=1000"; // + "&startTime=" + startTime;

        while (true) {
            if (!allCandlesticks.isEmpty()) {
                url = "https://api.binance.us/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=1000" + "&startTime=" + allCandlesticks.get(allCandlesticks.size() - 1).getOpenTime();
            }

            JSONArray data = new JSONArray();
            try {
                String response = ApiClient.makeAPICall(url);
                if (ApiClient.isValidJsonArr(response))
                    data = new JSONArray(response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<Candlestick> candlesticks = new ArrayList<>();

            for (int i = 0; i < data.length(); i++) {
                JSONArray stick = data.getJSONArray(i);
                candlesticks.add(new Candlestick(stick.getLong(0),
                        Double.parseDouble(stick.getString(1)),
                        Double.parseDouble(stick.getString(4))));
            }
            allCandlesticks.addAll(candlesticks);
            break;
//            if (candlesticks.size() < 999) {
//                break;
//            }
        }

        return allCandlesticks;
    }
}
