package com.market;

import com.BotRunner;
import com.constants.Constants;
import com.models.Candlestick;
import com.models.KlineDatapack;
import com.util.ApiClient;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Iterator;
import java.util.List;


public class MarketBot {
    private final static Logger LOGGER = Logger.getLogger(MarketBot.class);
    private final ApiClient apiClient;
    public static final double trailingPercentBase = 2;

    private final String name;
    private String coinSymbol = "";
    private double coinValue = 0;
    private double coinValuePaid = 0;
    private double coinValuePeak = 0;
    private double coinPercentChange = 0;
    private String lastSymbol = "temporary";
    private double trailingStopValue = 0;
    private double trailingPercent = trailingPercentBase;
    private String algoName = "default";
    private double numCoinsHeld = 0;
    private double accountVal = 1000;
    private double marketPerformance = 0;  //the overall com.market performance at time of last purchase

    private KlineDatapack klineDatapack;

    public MarketBot(String name) {
        this.name = name;
        loadCurrentValues();
        apiClient = new ApiClient();
    }

    public String getName() {
        return this.name;
    }

    public String getCoinSymbol() {
        return coinSymbol;
    }

    public double getCoinValue() {
        return coinValue;
    }

    public double getCoinPercentChange() {
        return coinPercentChange;
    }

    public double getAccountVal() {
        return accountVal;
    }

    public void runMarketBot(KlineDatapack klineData) {
        klineDatapack = klineData;
        if (coinSymbol.length() != 0) {
            updateCurrent();
            if (coinValue < trailingStopValue) {
                sellCurrent();
                buyNew(findNew(klineData));
            }
        } else {
            buyNew(findNew(klineData));
        }
        saveCurrentValues();
    }

    public void saveCurrentValues() {
        if (!BotRunner.isBacktest()) {
            JSONParser parser = new JSONParser();
            org.json.simple.JSONArray marketJsonArr = new org.json.simple.JSONArray();
            try (FileReader reader = new FileReader(Constants.botListFileName)) {
                marketJsonArr = (org.json.simple.JSONArray) parser.parse(reader);

                org.json.simple.JSONObject marketValues = null;
                int index = 0;
                for (Object m : marketJsonArr) {
                    org.json.simple.JSONObject market = (org.json.simple.JSONObject) m;
                    if (market.get("name").toString().equalsIgnoreCase(this.getName())) {
                        marketValues = market;
                        org.json.simple.JSONObject persist = (org.json.simple.JSONObject) marketValues.getOrDefault("persist", new org.json.simple.JSONObject());
                        persist.put("currentTicker", coinSymbol);
                        persist.put("lastTicker", lastSymbol);
                        persist.put("currentValue", coinValue);
                        persist.put("trailingPercent", trailingPercent);
                        persist.put("trailingStop", trailingStopValue);
                        persist.put("currentPeak", coinValuePeak);
                        persist.put("currentPaid", coinValuePaid);
                        persist.put("currentPerChange", coinPercentChange);
                        persist.put("numCoinsHeld", numCoinsHeld);
                        persist.put("accountVal", accountVal);
                        persist.put("marketPerformance", marketPerformance);

                        marketValues.put("persist", persist);
                        index = marketJsonArr.indexOf(market);
                    }
                }
                marketJsonArr.remove(index);
                marketJsonArr.add(index, marketValues);

            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
            try (FileWriter file = new FileWriter(Constants.botListFileName)) {
                file.write(new JSONArray(marketJsonArr.toJSONString()).toString(4));
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadCurrentValues() {
        JSONParser parser = new JSONParser();
        org.json.simple.JSONArray marketJsonArr;
        try (FileReader reader = new FileReader(Constants.botListFileName)) {
            marketJsonArr = (org.json.simple.JSONArray) parser.parse(reader);

            for (Object m : marketJsonArr) {
                org.json.simple.JSONObject market = (org.json.simple.JSONObject) m;
                if (market.get("name").toString().equalsIgnoreCase(this.getName())) {
                    algoName = market.getOrDefault("algoName", "default").toString();
                    market = (org.json.simple.JSONObject) market.getOrDefault("persist", new org.json.simple.JSONObject());
                    coinSymbol = market.getOrDefault("currentTicker", "").toString();
                    lastSymbol = market.getOrDefault("lastTicker", "temporary").toString();
                    coinValue = Double.parseDouble(market.getOrDefault("currentValue", 0).toString());
                    trailingPercent = Double.parseDouble(market.getOrDefault("trailingPercent", trailingPercentBase).toString());
                    trailingStopValue = Double.parseDouble(market.getOrDefault("trailingStop", 0).toString());
                    coinValuePeak = Double.parseDouble(market.getOrDefault("currentPeak", 0).toString());
                    coinValuePaid = Double.parseDouble(market.getOrDefault("currentPaid", 0).toString());
                    coinPercentChange = Double.parseDouble(market.getOrDefault("currentPerChange", 0).toString());
                    numCoinsHeld = Double.parseDouble(market.getOrDefault("numCoinsHeld", 0).toString());
                    accountVal = Double.parseDouble(market.getOrDefault("accountVal", 1000).toString());
                    marketPerformance = Double.parseDouble(market.getOrDefault("marketPerformance", 0).toString());
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public String findNew(KlineDatapack klineData) {
        Algorithms algos = new Algorithms(klineData);
        if (klineData == null)
            return "";
        if (name.equals("godBot")) {
            algoName = getGodAlgo();
        }
        return switch (algoName) {
            case "fib" -> algos.fib618();
            case "rsiAndFib" -> algos.rsiAndFib();
            case "MACD_RSI_130A" -> algos.RSI_MACD_PER(130, 14, 7, 25, 20, 99.5, 40, 101, 6, 10000);
            case "MACD_RSI_130B" -> algos.RSI_MACD_PER(130, 14, 14, 60, 20, 100, 40, 102, 6, 10000);
            case "MACD_RSI_130C" -> algos.RSI_MACD_PER(130, 14, 7, 25, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_130D" -> algos.RSI_MACD_PER(130, 14, 14, 60, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_127A" -> algos.RSI_MACD_PER(127, 14, 7, 25, 20, 99.5, 40, 101, 6, 10000);
            case "MACD_RSI_127B" -> algos.RSI_MACD_PER(127, 14, 14, 60, 20, 100, 40, 102, 6, 10000);
            case "MACD_RSI_127C" -> algos.RSI_MACD_PER(127, 14, 7, 25, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_127D" -> algos.RSI_MACD_PER(127, 14, 14, 60, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_125A" -> algos.RSI_MACD_PER(125, 14, 7, 25, 20, 99.5, 40, 101, 6, 10000);
            case "MACD_RSI_125B" -> algos.RSI_MACD_PER(125, 14, 14, 60, 20, 100, 40, 102, 6, 10000);
            case "MACD_RSI_125C" -> algos.RSI_MACD_PER(125, 14, 7, 25, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_125D" -> algos.RSI_MACD_PER(125, 14, 14, 60, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_122A" -> algos.RSI_MACD_PER(122, 14, 7, 25, 20, 99.5, 40, 101, 6, 10000);
            case "MACD_RSI_122B" -> algos.RSI_MACD_PER(122, 14, 14, 60, 20, 100, 40, 102, 6, 10000);
            case "MACD_RSI_122C" -> algos.RSI_MACD_PER(122, 14, 7, 25, 20, 0, 40, 0, 6, 10000);
            case "MACD_RSI_122D" -> algos.RSI_MACD_PER(122, 14, 14, 60, 20, 0, 40, 0, 6, 10000);
            case "40RSI_Fib03" -> algos.lowRSIanyFib(40, .03, 10000);
            case "30RSI_Fib01" -> algos.lowRSIanyFib(30, .01, 10000);
            case "30RSI_Fib02" -> algos.lowRSIanyFib(30, .02, 10000);
            case "30RSI_Fib03" -> algos.lowRSIanyFib(30, .03, 10000);
            case "28RSI_Fib01" -> algos.lowRSIanyFib(28, .01, 10000);
            case "28RSI_Fib02" -> algos.lowRSIanyFib(28, .02, 10000);
            case "28RSI_Fib03" -> algos.lowRSIanyFib(28, .03, 10000);
            case "25RSI_Fib01" -> algos.lowRSIanyFib(25, .01, 10000);
            case "25RSI_Fib02" -> algos.lowRSIanyFib(25, .02, 10000);
            case "25RSI_Fib03" -> algos.lowRSIanyFib(25, .03, 10000);
            default -> algos.rsiLT30();
        };
    }

    private void buyNew(String newTicker) {
        if (!newTicker.equals("")) {
            coinSymbol = newTicker;
            accountVal -= (accountVal * ((Constants.feePercent / 100) / 2));
            updateCurrent();
            trailingStopValue = (coinValue - ((trailingPercentBase / 100.0) * coinValue));
            marketPerformance = MarketDataHandler.getMarketPerformance();
            saveCurrentValues();
            String message = "[" + this.getName() + "] (" + this.algoName + ") Bought " + coinSymbol + " at $" + coinValue + " [https://www.binance.us/en/trade/pro/" + coinSymbol + "]";
            LOGGER.info(message);
            if (!BotRunner.isBacktest())
                BotRunner.UPDATER.sendUpdateMsg(message);
        }
    }

    public void updateCurrent() {
        if (BotRunner.isBacktest()) {
            List<Candlestick> tickerKline = klineDatapack.getKline1mData().get(coinSymbol);
            if (tickerKline == null) {
                LOGGER.info("No Data for " + coinSymbol);
                return;
            }
            coinValue = tickerKline.get(tickerKline.size() - 1).getClose();
            if (coinValuePaid == 0) { //just bought this
                coinValuePaid = coinValue;
                numCoinsHeld = accountVal / coinValue;
                trailingStopValue = (coinValue - ((trailingPercentBase / 100.0) * coinValue));
            }
            accountVal = numCoinsHeld * coinValue;
            coinPercentChange = ((100 / coinValuePaid) * coinValue) - 100;

            if (coinValue > coinValuePeak) {
                coinValuePeak = coinValue;
                trailingPercent = trailingPercentBase;
                if (coinPercentChange > .6) {                                             //at .6% gain, set trail down to .5%
                    trailingPercent = .5;
                }
                if (coinPercentChange > 1) {                                             //trail stays at .5% until 1% gain
                    trailingPercent += (coinPercentChange - 1) * .1;                 //trail grows .1% for each addition 1% gain
                    if (trailingPercent > 3) {
                        trailingPercent = 3;
                    }
                }
                trailingStopValue = (coinValuePeak - ((trailingPercent / 100.0) * coinValuePeak));
            }
        } else {
            String url = "https://www.binance.us/api/v3/ticker/price?symbol=" + coinSymbol;
            try {
                String JSON_DATA = apiClient.makeAPICall(url);
                JSONObject data = new JSONObject(JSON_DATA);

                coinValue = data.getDouble("price");
                if (coinValuePaid == 0) { //just bought this
                    coinValuePaid = coinValue;
                    numCoinsHeld = accountVal / coinValue;
                    trailingStopValue = (coinValue - ((trailingPercentBase / 100.0) * coinValue));
                }
                accountVal = numCoinsHeld * coinValue;
                coinPercentChange = ((100 / coinValuePaid) * coinValue) - 100;

                if (coinValue > coinValuePeak) {
                    coinValuePeak = coinValue;
                    trailingPercent = trailingPercentBase;
                    if (coinPercentChange > .6) {                                             //at .6% gain, set trail down to .5%
                        trailingPercent = .5;
                    }
                    if (coinPercentChange > 1) {                                             //trail stays at .5% until 1% gain
                        trailingPercent += (coinPercentChange - 1) * .1;                 //trail grows .1% for each addition 1% gain
                        if (trailingPercent > 3) {
                            trailingPercent = 3;
                        }
                    }
                    trailingStopValue = (coinValuePeak - ((trailingPercent / 100.0) * coinValuePeak));
                }
            } catch (IOException | JSONException e) {
                LOGGER.error("Error: cannot access content - " + e.toString());
            }
        }
    }

    public void sellCurrent() {
        String message = "[" + this.getName() + "] (" + this.algoName + ") Sold " + coinSymbol + " at $" + Constants.decimalFormat.format(coinValue) + " (" + Constants.decimalFormat.format(coinPercentChange) + "%)";
        LOGGER.info(message);
        if (!BotRunner.isBacktest())
            BotRunner.UPDATER.sendUpdateMsg(message);
        sellLogAdd();
        lastSymbol = coinSymbol;
        coinValue = 0;
        coinSymbol = "";
        trailingPercent = trailingPercentBase;
        trailingStopValue = 0;
        coinValuePeak = 0;
        coinValuePaid = 0;
        coinPercentChange = 0;
        numCoinsHeld = 0;
        marketPerformance = 0;
        accountVal -= (accountVal * ((Constants.feePercent / 100) / 2));
        saveCurrentValues();
    }

    private void sellLogAdd() {
        String d = LocalDateTime.now().format(Constants.longDateFormat);
        if (BotRunner.isBacktest()) {
            List<Candlestick> tickerKline = klineDatapack.getKline1mData().get(coinSymbol);
            Long closeTime = tickerKline.get(tickerKline.size() - 1).getCloseTime();
            SimpleDateFormat dt = new SimpleDateFormat("MM/dd/yyyy hh:mm a");
            d = dt.format(new Date(closeTime));
        }
        try {
            File dir = new File("sellLogs");
            dir.mkdir();
            File file = new File(dir, this.name + "_sellLog.txt");
            FileWriter fw = new FileWriter(file, true);
            if (file.length() != 0) {
                fw.write("\n");
            }
            fw.write("(" + d + ") Sold " + coinSymbol + " (" + Constants.decimalFormat.format(coinPercentChange) + "%) Market Performance @ buy: [" + Constants.decimalFormat.format(marketPerformance) + "]");
            fw.close();
        } catch (IOException e) {
            LOGGER.error("Failed to write to sellLog. Error: " + e.getMessage());
        }
    }

    public void resetBot() {
        this.coinSymbol = "";
        this.coinValue = 0;
        this.coinValuePaid = 0;
        this.coinValuePeak = 0;
        this.coinPercentChange = 0;
        this.lastSymbol = "temporary";
        this.trailingStopValue = 0;
        this.trailingPercent = trailingPercentBase;
        this.numCoinsHeld = 0;
        this.accountVal = 1000;
        this.marketPerformance = 0;
        saveCurrentValues();
        File file = new File("sellLogs", this.name + "_sellLog.txt");
        file.delete();
    }

    private String getGodAlgo() {
        String algo = "";
        double mp = MarketDataHandler.getMarketPerformance();
        for (int i = 0; i < Constants.mpRanges.length; i++) {
            if (i == 0) {
                if (mp > Constants.mpRanges[i]) {
                    algo = Constants.godBotAlgos[i];
                }
            } else if (i == Constants.mpRanges.length - 1) {
                if ((mp >= Constants.mpRanges[i]) && (mp < Constants.mpRanges[i - 1])) {
                    algo = Constants.godBotAlgos[i];
                } else if (mp < Constants.mpRanges[i]) {
                    algo = Constants.godBotAlgos[i + 1];
                }
            } else if ((mp >= Constants.mpRanges[i]) && (mp < Constants.mpRanges[i - 1])) {
                algo = Constants.godBotAlgos[i];
            }
        }
        return algo;
    }
/*    public static void updateBinanceTickers() {
        String url = "https://www.binance.us/gateway-api/v2/public/asset-service/product/get-products?includeEtf=false";

        try {
            String JSON_DATA = makeAPICall(url);
            JSONObject whole = new JSONObject(JSON_DATA);
            JSONArray data = whole.getJSONArray("data");

            for (int i = 0; i < data.length() - 1; i++) {
                String testTicker = data.getJSONObject(i).getString("s");
                if (testTicker.endsWith("USD")) {
                    String filename = "\\allowed.txt";
                    FileWriter fw = new FileWriter(filename, true);
                    fw.write("\n" + testTicker);
                    fw.close();
                    LOGGER.info(testTicker + " added to list.");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error: cannot access content - " + e.toString());
        } catch (URISyntaxException e) {
            LOGGER.error("Error: Invalid URL " + e.toString());
        }
        return;
    }*/
}