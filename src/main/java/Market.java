import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

public class Market {
    private final static Logger LOGGER = Logger.getLogger(Market.class);
    private static final DecimalFormat df = new DecimalFormat("#.###");
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
    private static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("hh:mm a");
    private String[] allowedTickers;

    private final double trailingPercentBase = 2;   //the percent you want the stop loss trail to start at

    private String coinSymbol = "";
    private double coinValue = 0;
    private double coinValuePaid = 0;
    private double coinValuePeak = 0;
    private double coinPercentChange = 0;
    private String lastSymbol = "temporary";
    private double trailingStopValue = 0;
    private double trailingPercent = trailingPercentBase;

    Market() {
        loadCurrentValues();
    }

    public String getCoinSymbol() {
        return coinSymbol;
    }

    private int updateCycleCounter = 1;

    public void MarketBot() {
        String d = LocalDateTime.now().format(formatter2);
        LOGGER.info("\nRan test at " + d);

        if (coinSymbol.length() != 0) {
            updateCurrent();
            if (coinValue < trailingStopValue) {
                while (coinSymbol.length() != 0) {
                    sellCurrent();
                }
                while (coinSymbol.length() == 0) {
                    buyNew(findNew());
                }
            } else {
                LOGGER.info("Holding " + coinSymbol + ": " + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%) [Paid: " + coinValuePaid + " Trail: " + df.format(trailingStopValue) + "] (" + trailingPercent + "%)");
                if (updateCycleCounter < 1) {
                    Main.UPDATER.sendUpdateMsg("```(" + d + ") " + coinSymbol + ": " + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%)```");
                    updateCycleCounter = 60 / Main.CYCLE_TIME; //only send discord updates every minute
                } else {
                    updateCycleCounter--;
                }
                saveCurrentValues();
            }
        } else {
            while (coinSymbol.length() == 0) {
                buyNew(findNew());
            }
        }
    }

    public void saveCurrentValues() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        prefs.put("currentTicker", coinSymbol);
        prefs.put("lastTicker", lastSymbol);
        prefs.putDouble("currentValue", coinValue);
        prefs.putDouble("trailingPercent", trailingPercent);
        prefs.putDouble("trailingStop", trailingStopValue);
        prefs.putDouble("currentPeak", coinValuePeak);
        prefs.putDouble("currentPaid", coinValuePaid);
        prefs.putDouble("currentPerChange", coinPercentChange);
    }

    private void loadCurrentValues() {
        Preferences prefs = Preferences.userNodeForPackage(Main.class);
        coinSymbol = prefs.get("currentTicker", "");
        lastSymbol = prefs.get("lastTicker", "temporary");
        coinValue = prefs.getDouble("currentValue", 0);
        trailingPercent = prefs.getDouble("trailingPercent", trailingPercentBase);
        trailingStopValue = prefs.getDouble("trailingStop", 0);
        coinValuePeak = prefs.getDouble("currentPeak", 0);
        coinValuePaid = prefs.getDouble("currentPaid", 0);
        coinPercentChange = prefs.getDouble("currentPerChange", 0);

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
            LOGGER.error("Error reading allowed coins file. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String makeAPICall(String url) throws IOException {
        String response_content;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);

        try (CloseableHttpResponse response = client.execute(request)) {
            HttpEntity entity = response.getEntity();
            response_content = EntityUtils.toString(entity);
            EntityUtils.consume(entity);
        }
        return response_content;
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
                data = new JSONArray(makeAPICall(url));
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

    private long getDateDeltaUnix(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);

        return cal.getTime().getTime();
    }

    /*ALGO GOES HERE*/
    public String findNew() {
        return "DOGECOIN";
    }

    private void buyNew(String newTicker) {
        if (!newTicker.equals("")) {
            //BUY CODE GOES HERE
            if (tradeConfirm(newTicker)) {
                coinSymbol = newTicker;
                trailingPercent = trailingPercentBase;
                updateCurrent();
                trailingStopValue = (coinValue - ((trailingPercentBase / 100.0) * coinValue));
                updateCurrent();
                String message = "Bought " + coinSymbol + " at $" + coinValue + " [https://www.binance.us/en/trade/pro/" + coinSymbol + "]";
                LOGGER.info(message);
                Main.UPDATER.sendUpdateMsg(message);
            }
        }
        saveCurrentValues();
    }

    public void updateCurrent() {
        String url = "https://www.binance.us/api/v3/ticker/price?symbol=" + coinSymbol;
        try {
            String JSON_DATA = makeAPICall(url);
            JSONObject data = new JSONObject(JSON_DATA);
            for (Iterator it = data.keys(); it.hasNext(); ) {
                String key = (String) it.next();
                if (key.equals("price")) {
                    coinValue = data.getDouble(key);
                    if (coinValuePaid == 0) {
                        coinValuePaid = coinValue;
                    }
                    coinPercentChange = ((100 / coinValuePaid) * coinValue) - 100;
                    break;
                }
            }
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
        } catch (IOException e) {
            LOGGER.error("Error: cannot access content - " + e.toString());
        }
    }

    public void sellCurrent() {
        //SELL CODE GOES HERE
        if (tradeConfirm("")) {
            String message = "Sold " + coinSymbol + " at $" + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%)";
            LOGGER.info(message);
            Main.UPDATER.sendUpdateMsg(message);

            String d = LocalDateTime.now().format(formatter);
            try {
                File file = new File("sellLog.txt");
                FileWriter fw = new FileWriter(file, true);
                if (file.length() != 0) {
                    fw.write("\n");
                }
                fw.write("(" + d + ") Sold " + coinSymbol + " (" + df.format(coinPercentChange) + "%)");
                fw.close();
            } catch (IOException e) {
                LOGGER.error("Failed to write to sellLog. Error: " + e.getMessage());
            }
            lastSymbol = coinSymbol;
            coinValue = 0;
            coinSymbol = "";
            trailingPercent = trailingPercentBase;
            trailingStopValue = 0;
            coinValuePeak = 0;
            coinValuePaid = 0;
            coinPercentChange = 0;
            saveCurrentValues();
        }
    }

    private boolean tradeConfirm(String s) {
     /*  boolean confirmed = false;
       if(s.equals("")){
            confirm it sold
            confirmed = true;
        }
        else{
            confirm it bought 's'
            confirmed = true;
        } */
        return true; //confirmed
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
                    String filename = "c:\\Users\\Shaftspin\\Desktop\\IntelliJ\\IntelliJ IDEA Community Edition 2020.3.3\\AlgoTest\\allowed.txt";
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