
import enums.KlineInterval;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    public static final int CYCLE_TIME = 15;       //run the market test every X seconds
    public static final int UPDATE_CYCLE_TIME = 1; //how many minutes between each discord update (ends up taking longer because kline stuff)
    public final static double feePercent = 1;     //estimated total fee as a % - per transactions (both buy/sell and spread)

    private static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DecimalFormat df = new DecimalFormat("#.###");

    public static DiscordBot UPDATER = new DiscordBot();
    public static List<Market> MARKETS = createBotsList();

    public static final String botListFile = "src/main/resources/BotList.json";
    public static final boolean persistData = true;

    private static boolean busy = false;         //a check to keep commands from interrupting runMarketBot procedures
    public static boolean getBusyMarket() {return busy;}

    public static void main(String[] args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                KlineDatapack klineData = getKlineData();
                marketPerformance = calculateMarketPerformance(klineData, 120);
                busy = true;
                MARKETS.forEach(market -> market.runMarketBot(klineData));
                if (updateCtr <= 1) {
                    updates(MARKETS);
                    updateCtr = ((UPDATE_CYCLE_TIME * 60) / CYCLE_TIME);
                } else {
                    updateCtr--;
                }
                busy = false;
            }
        }, 0, 1000L * CYCLE_TIME);
    }

    public static List<Market> createBotsList() {
        JSONParser parser = new JSONParser();
        List<Market> marketBots = new ArrayList<>();
        try (FileReader reader = new FileReader(botListFile)) {
            JSONArray marketJsonArr = (JSONArray) parser.parse(reader);
            marketJsonArr.forEach(m -> {
                JSONObject market = (JSONObject) m;
                marketBots.add(new Market(market.get("name").toString()));
                UPDATER.sendUpdateMsg(market.get("name").toString() + " Started.");
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return marketBots;
    }

    public static int updateCtr = 1;
    private static void updates(List<Market> bots) {
        String d = LocalDateTime.now().format(formatter2);
        String msg = "```<" + d + "> STATUS:";
        for (Market s : bots) {
            msg += "\n";
            if (s.getCoinSymbol().equals("")) {
                msg += "[" + s.getName() + "] " + "Searching...";
            } else {
                msg += "[" + s.getName() + "] " + s.getCoinSymbol() + " " + df.format(s.getCoinValue()) + " (" + df.format(s.getCoinPercentChange()) + "%)";
            }
        }
        msg += "```";
        Main.UPDATER.sendUpdateMsg(msg);
    }

    public static KlineDatapack getKlineData() {
        MarketUtil marketUtil = new MarketUtil();
        KlineDatapack klineData = new KlineDatapack();
        Map<String, List<Candlestick>> kline1m = marketUtil.getKlineForAllTickers(KlineInterval.ONE_MINUTE);
        klineData.setKline1mData(kline1m);
        return klineData;
    }

    private static double marketPerformance = 0;
    public static double getMarketPerformance() {return marketPerformance;}

    private static double calculateMarketPerformance(KlineDatapack data, int rangeInMinutes){
        double totalVol = 0;
        double weightPts = 0;
        for (String ticker : MarketUtil.allowedTickers) {
            List<Candlestick> klineData = data.getKline1mData().get(ticker);
            if (klineData.isEmpty())
                continue;
            double start = klineData.get(klineData.size()-1-rangeInMinutes).getClose();
            double end = klineData.get(klineData.size()-1).getClose();
            double percentChange = ((end-start)/start) * 100;
            double volume = MarketUtil.getUSDVolumeAvg(klineData,rangeInMinutes);

            totalVol += volume;
            weightPts += percentChange * volume;
        }
        return weightPts/totalVol;
    }
}

//TODO: algo stuff
//RSI seems off?
//Scale acceptable buys based on market conditions, positive tend = slightly looser reqs
//Block things that had any major jumps in price?
//Volatility implementing
//Implement sentiment scalping?
//Limit gains per day? (stop trading after x% gain (or loss) on a day?)

