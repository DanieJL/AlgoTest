package com;

import com.constants.Constants;
import com.discord.DiscordHandler;
import com.enums.KlineInterval;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.MarketBot;
import com.market.MarketDataHandler;
import com.models.Candlestick;
import com.models.KlineDatapack;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class BotRunner {
    public static final DiscordHandler UPDATER = new DiscordHandler();
    public static final List<MarketBot> MARKETBots = createBotsList();
    private final static Logger LOGGER = Logger.getLogger(BotRunner.class);

    private static boolean pauseMarket = false;
    private static final boolean backtest = true;
    private static final boolean backtestFromFile = true;
    private static final String backtestFile = "5_17_2021_data.json";
    private static final int backtestDateDeltaInDays = 30;

    private static boolean busy = false;
    private static final KlineInterval backtestInterval = KlineInterval.ONE_MINUTE;

    public static void main(String[] args) {

        MarketDataHandler marketDataHandler = new MarketDataHandler();

        //Uncomment this and run to generate a new backtesting datafile.
//        marketDataHandler.generateBacktestDataFile(backtestFile,
//                365,
//                0,
//                KlineInterval.ONE_MINUTE);
//        System.exit(0);

        if (isBacktest()) {
            backtestRun(marketDataHandler);
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    realTimeRun(marketDataHandler);
                }
            }, 0, 1000L * Constants.CYCLE_TIME);
        }
    }

    public static boolean isBacktest() {
        return backtest;
    }

    public static boolean getBusyMarket() {
        return busy;
    }

    public static boolean getPauseMarket() {
        return pauseMarket;
    }

    public static void setPauseMarket(boolean choice) {
        pauseMarket = choice;
    }

    public static List<MarketBot> createBotsList() {
        JSONParser parser = new JSONParser();
        List<MarketBot> marketBotBots = new ArrayList<>();
        try (FileReader reader = new FileReader(Constants.botListFileName)) {
            JSONArray marketJsonArr = (JSONArray) parser.parse(reader);
            marketJsonArr.forEach(m -> {
                JSONObject market = (JSONObject) m;
                marketBotBots.add(new MarketBot(market.get("name").toString()));
                UPDATER.sendUpdateMsg(market.get("name").toString() + " Started.");
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return marketBotBots;
    }

    private static void realTimeRun(MarketDataHandler marketDataHandler) {
        if (!pauseMarket) {
            KlineDatapack klineData = marketDataHandler.getKlineData(0, 0);
            marketDataHandler.setMarketPerformance(klineData, Constants.mpCalcRange);
            busy = true;
            MARKETBots.forEach(marketBot -> marketBot.runMarketBot(klineData));
            marketDataHandler.handleUpdates(MARKETBots);
            busy = false;
        }
    }

    private static void backtestRun(MarketDataHandler marketDataHandler) {
        busy = true;
        for (MarketBot marketBot : MARKETBots) {
            marketBot.resetBot();
        }
        UPDATER.sendUpdateMsg("Backtest in progress.. Please do not send commands until completion confirmed.");

        KlineDatapack klineData = null;
        if (backtestFromFile) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                klineData = objectMapper.readValue(new File(new File("backtestData"), backtestFile),
                        KlineDatapack.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            klineData = marketDataHandler.getBacktestData(backtestDateDeltaInDays, 0, backtestInterval);
        }

        KlineDatapack intervalKeeperPack = new KlineDatapack();
        int iterations = klineData.getKline1mData().get(MarketDataHandler.allowedTickers[0]).size();
        for (int i = 0; i < iterations; i++) {
            if (i % 1000 == 0) {
                LOGGER.info("On iteration " + i + " / " + iterations);
            }
            Map<String, List<Candlestick>> incrementedData = klineData.getKline1mDataIncremented(i);
            if (incrementedData.isEmpty()) {
                LOGGER.info("End of backtest.");
                break;
            }
            intervalKeeperPack.setKline1mData(incrementedData);
            marketDataHandler.setMarketPerformance(intervalKeeperPack, Constants.mpCalcRange);
            MARKETBots.forEach(marketBot -> marketBot.runMarketBot(intervalKeeperPack));
        }
        busy = false;
        UPDATER.sendUpdateMsg("Backtest Completed.");
    }
}
