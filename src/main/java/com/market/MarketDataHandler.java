package com.market;

import com.BotRunner;
import com.constants.Constants;
import com.enums.KlineInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.models.Candlestick;
import com.models.KlineDatapack;
import com.util.ApiClient;
import com.util.GeneralUtil;
import com.util.MarketUtil;
import org.apache.log4j.Logger;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MarketDataHandler {
    private final static Logger LOGGER = Logger.getLogger(MarketDataHandler.class);
    private static double marketPerformance;
    private int updateCtr = 1;

    public static String[] allowedTickers;
    private final ApiClient apiClient;

    public MarketDataHandler() {
        apiClient = new ApiClient();
    }

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

    public KlineDatapack getKlineData(int daysAgoStart, int daysAgoEnd) {
        KlineDatapack klineData = new KlineDatapack();
        Map<String, List<Candlestick>> kline1m = getKlineForAllTickers(KlineInterval.ONE_MINUTE, daysAgoStart, daysAgoEnd);
        klineData.setKline1mData(kline1m);
        return klineData;
    }

    public static double getMarketPerformance() {
        return marketPerformance;
    }

    public void setMarketPerformance(KlineDatapack data, int rangeInMinutes) {
        double totalVol = 0;
        double weightPts = 0;
        for (String ticker : allowedTickers) {
            List<Candlestick> klineData = data.getKline1mData().get(ticker);
            if (klineData == null || klineData.isEmpty())
                continue;
            double start = klineData.get(klineData.size() - 1 - rangeInMinutes).getClose();
            double end = klineData.get(klineData.size() - 1).getClose();
            double percentChange = ((end - start) / start) * 100;
            double volume = MarketUtil.getUSDVolumeAvg(klineData, rangeInMinutes);

            totalVol += volume;
            weightPts += percentChange * volume;
        }
        marketPerformance = weightPts / totalVol;
    }

    public KlineDatapack getBacktestData(int backtestDateDeltaInDays, int daysAgoEnd, KlineInterval backtestInterval) {
        KlineDatapack klineData = new KlineDatapack();
        Map<String, List<Candlestick>> data = getKlineForAllTickers(backtestInterval, backtestDateDeltaInDays, daysAgoEnd);
        klineData.setKlineData(data, backtestInterval);

        return klineData;
    }

    public void generateBacktestDataFile(String filename, int startDateDaysBack, int endDateDaysBack, KlineInterval interval) {
        KlineDatapack klineData = getBacktestData(startDateDaysBack, endDateDaysBack, interval);
        ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        try {
            File dir = new File("backtestData");
            dir.mkdir();
            objectMapper.writeValue(new File(dir, filename), klineData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleUpdates(List<MarketBot> bots) {
        if (updateCtr <= 1) {
            String d = LocalDateTime.now().format(Constants.shortDateFormat);
            StringBuilder msg = new StringBuilder("[" + d + "] Status update: (MP: " + Constants.decimalFormat.format(marketPerformance) + "%)\n```");
            for (MarketBot s : bots) {
                msg.append("\n");
                if (s.getCoinSymbol().equals("")) {
                    msg.append("[").append(s.getName()).append("] ").append("Searching...");
                } else {
                    msg.append("[").append(s.getName()).append("] ")
                            .append(s.getCoinSymbol())
                            .append(" ")
                            .append(Constants.decimalFormat.format(s.getCoinValue()))
                            .append(" (").append(Constants.decimalFormat.format(s.getCoinPercentChange())).append("%)");
                }
            }
            msg.append("```");
            BotRunner.UPDATER.sendUpdateMsg(msg.toString());
            updateCtr = ((Constants.UPDATE_CYCLE_TIME * 60) / Constants.CYCLE_TIME);
        } else {
            updateCtr--;
        }
    }

    public List<Candlestick> getKlineData(String symbol, String interval, int daysAgoStart, int daysAgoEnd) {
        List<Candlestick> allCandlesticks = new ArrayList<>();
        String url = "https://api.binance.us/api/v3/klines?symbol=" + symbol + "&interval=" + interval + "&limit=1000";
        if (daysAgoStart != 0) {
            long startTime = GeneralUtil.getDateDeltaUnix(-(daysAgoStart + 1));
            url += "&startTime=" + startTime;
        }
        long endTime = 0;
        if (daysAgoEnd != 0) {
            endTime = GeneralUtil.getDateDeltaUnix(-(daysAgoEnd));
            url += "&endTime=" + endTime;
        }

        while (true) {
            if (!allCandlesticks.isEmpty()) {
                url = "https://api.binance.us/api/v3/klines?symbol=" + symbol +
                        "&interval=" + interval +
                        "&limit=1000" +
                        "&startTime=" + allCandlesticks.get(allCandlesticks.size() - 1).getOpenTime();
                if (endTime != 0)
                    url += "&endTime=" + endTime;
            }

            JSONArray data = new JSONArray();
            try {
                String response = apiClient.makeAPICall(url);
                if (apiClient.isValidJsonArr(response))
                    data = new JSONArray(response);
            } catch (IOException e) {
                e.printStackTrace();
            }

            List<Candlestick> candlesticks = new ArrayList<>();

            for (int i = 0; i < data.length(); i++) {
                JSONArray stick = data.getJSONArray(i);
                candlesticks.add(new Candlestick(stick.getLong(0),
                        Double.parseDouble(stick.getString(1)),
                        Double.parseDouble(stick.getString(4)),
                        Double.parseDouble(stick.getString(7)),
                        stick.getLong(6)));
            }
            allCandlesticks.addAll(candlesticks);
            if (daysAgoStart == 0 || candlesticks.size() < 999)
                break;
        }

        return allCandlesticks;
    }

    public Map<String, List<Candlestick>> getKlineForAllTickers(KlineInterval interval, int daysAgoStart, int daysAgoEnd) {
        Map<String, List<Candlestick>> klineMap = new HashMap<>();
        for (String ticker : allowedTickers) {
            LOGGER.info("Getting kline data for " + ticker);
            List<Candlestick> klines = getKlineData(ticker, interval.getInterval(), daysAgoStart, daysAgoEnd);
            klineMap.put(ticker, klines);
        }
        return klineMap;
    }
}
