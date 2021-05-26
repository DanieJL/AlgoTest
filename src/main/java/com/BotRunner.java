package com;

import com.constants.Constants;
import com.discord.DiscordHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.Demo;
import com.market.MarketBot;
import com.market.MarketDataHandler;
import com.market.MultithreadBotRunner;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BotRunner {
    public static final DiscordHandler UPDATER = new DiscordHandler();
    public static List<MarketBot> MARKETBots = createBotsList();
    private final static Logger LOGGER = Logger.getLogger(BotRunner.class);

    private static boolean pauseMarket = true;
    private static final boolean backtest = true;
    private static final String backtestFile = "1_1_2021to5_18_2021.json";
    public static final boolean createFile = false;

    private static boolean busy = false;
    public static final boolean multithread = true; //Only applies to backtesting

    public static void main(String[] args) {
        Demo d = new Demo();
/*        if (createFile) {
            UPDATER.sendUpdateMsg("CREATING: [" + backtestFile + "]");
            new MarketDataHandler().generateBacktestDataFile(backtestFile,
                    41,
                    34,
                    KlineInterval.ONE_MINUTE);
            UPDATER.sendUpdateMsg("FINISHED: [" + backtestFile + "]");
            System.exit(0);
        }

        if (isBacktest()) {
            long startTime = System.nanoTime();
            backtestRun();
            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1000000;
            UPDATER.sendUpdateMsg("Took " + duration + "ms to execute.");
        } else {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    realTimeRun();
                }
            }, 0, 1000L * Constants.CYCLE_TIME);
        }*/
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
                //if(!BotRunner.createFile) {UPDATER.sendUpdateMsg(market.get("name").toString() + " Started.");};
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return marketBotBots;
    }

    private static void realTimeRun() {
        MarketDataHandler marketDataHandler = new MarketDataHandler();
        if (!pauseMarket) {
            KlineDatapack klineData = marketDataHandler.getKlineData(0, 0);
            marketDataHandler.setMarketPerformance(klineData, Constants.mpCalcRange);
            busy = true;
            MARKETBots.forEach(marketBot -> marketBot.runMarketBot(klineData));
            marketDataHandler.handleUpdates(MARKETBots);
            busy = false;
        }
    }

    public static void backtestRun() {
        busy = true;
        for (MarketBot marketBot : MARKETBots) {
            marketBot.resetBot();
        }
        //UPDATER.sendUpdateMsg("Backtesting [" + backtestFile + "]...\nPlease do not send commands until completion confirmed.");

        KlineDatapack klineData = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            klineData = objectMapper.readValue(new File(new File("backtestData"), backtestFile),
                    KlineDatapack.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (multithread) {
            List<MultithreadBotRunner> botThreads = new ArrayList<>();
            for (MarketBot marketBot : MARKETBots) {
                MarketDataHandler marketDataHandler = new MarketDataHandler();
                botThreads.add(new MultithreadBotRunner(marketBot, klineData, marketDataHandler));
            }
            botThreads.forEach(MultithreadBotRunner::start);
        } else {
            MarketDataHandler marketDataHandler = new MarketDataHandler();
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
        }
        busy = false;
        //UPDATER.sendUpdateMsg("Backtest Completed.");
    }
}
