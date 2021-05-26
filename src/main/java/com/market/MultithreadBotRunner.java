package com.market;

import com.constants.Constants;
import com.models.Candlestick;
import com.models.KlineDatapack;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class MultithreadBotRunner implements Runnable {
    private final static Logger LOGGER = Logger.getLogger(MultithreadBotRunner.class);

    private MarketDataHandler marketDataHandler;
    private MarketBot marketBot;
    private KlineDatapack klineData;
    private Thread t;

    public MultithreadBotRunner(MarketBot marketBot, KlineDatapack klineData, MarketDataHandler marketDataHandler) {
        this.marketDataHandler = marketDataHandler;
        this.marketBot = marketBot;
        this.klineData = klineData;
    }

    @Override
    public void run() {
        KlineDatapack intervalKeeperPack = new KlineDatapack();
        int iterations = klineData.getKline1mData().get(MarketDataHandler.allowedTickers[0]).size();
        //for (int i = 0; i < iterations; i++) {
        for (int i = (iterations - (Demo.testStartDaysBack*1440)); i < (iterations - (Demo.testEndDaysBack*1440)); i++) {
            if (i % 1000 == 0) {
                LOGGER.info("Thread " + marketBot.getName());
                LOGGER.info("On iteration " + i + " / " + iterations);
            }
            Map<String, List<Candlestick>> incrementedData = klineData.getKline1mDataIncremented(i);
            if (incrementedData.isEmpty()) {
                LOGGER.info("End of backtest.");
                break;
            }
            intervalKeeperPack.setKline1mData(incrementedData);
            marketDataHandler.setMarketPerformance(intervalKeeperPack, Constants.mpCalcRange);
            marketBot.runMarketBot(intervalKeeperPack);
        }
        LOGGER.info("Thread " + marketBot.getName() + " completed.");
    }

    public void start() {
        if (t == null) {
            t = new Thread(this);
            t.start();
        }
    }
}
