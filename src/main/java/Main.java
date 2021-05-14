
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

    public static void main(String[] args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                KlineDatapack klineData = getKlineData();
                MARKETS.forEach(market -> market.runMarketBot(klineData));
                if (updateCtr <= 1) {
                    updates(MARKETS);
                    updateCtr = ((UPDATE_CYCLE_TIME * 60) / CYCLE_TIME);
                } else {
                    updateCtr--;
                }

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
        if (MARKETS.stream().anyMatch(market -> market.getCoinSymbol().equalsIgnoreCase(""))) {
            Map<String, List<Candlestick>> kline4h = marketUtil.getKlineForAllTickers(KlineInterval.FOUR_HOUR);
            klineData.setKline4hData(kline4h);
        }

        return klineData;
    }
}

//TODO: algo stuff
//RSI seems off?
//Scale acceptable buys based on market conditions, positive tend = slightly looser reqs
//Block things that had any major jumps in price?
//Implement sentiment scalping?
//Filter for more recent volume requirements?
//Limit gains per day? (stop trading after x% gain (or loss) on a day?)

