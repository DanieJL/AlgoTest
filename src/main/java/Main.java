
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
    private static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DecimalFormat df = new DecimalFormat("#.###");

    public static DiscordBot UPDATER = new DiscordBot();
    public static List<Market> MARKETS = createBotsList();

    public static final String botListFile = "src/main/resources/BotList.json";
    public static final boolean persistData = true;

    public static int updateCtr = 1;
    public static void main(String[] args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                MARKETS.forEach(Market::runMarketBot);
                if(updateCtr<=1){
                    updates(MARKETS);
                    updateCtr = ((UPDATE_CYCLE_TIME*60)/CYCLE_TIME);
                } else {updateCtr--;}

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

    private static void updates(List<Market> bots){
        String d = LocalDateTime.now().format(formatter2);
        String msg = "```<" + d + ">";
        for(Market s : bots){
            msg+= "\n";
            if(s.getCoinSymbol().equals("")){
                msg += "[" + s.getName() + "] " + "Searching...";
            }
            else{
                msg += "[" + s.getName() + "] " + s.getCoinSymbol() + " " + df.format(s.getCoinValue()) + " (" + df.format(s.getCoinPercentChange()) + "%)";
            }
        }
        msg += "```";
        Main.UPDATER.sendUpdateMsg(msg);
    }
}

//TODO: stuff

//-- ConfigHandler added for this, add bot.config to resources folder
//--format is key=value

//Implement actual trading

//**ALGO STUFF**
//RSI seems off?
//Scale acceptable buys based on market conditions, positive tend = slightly looser reqs
//Block things that had any major jumps in price?
//Implement sentiment scalping?
//Filter for more recent volume requirements?
//Limit gains per day? (stop trading after x% gain (or loss) on a day?)

//have multiple market objects running different variables see how each perform.

