import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

public class Market {
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

    Market(){loadCurrentValues();}
    public String getCoinSymbol() {return coinSymbol;}

    private int updateCycleCounter = 1;
    public void MarketBot() {
        String d = LocalDateTime.now().format(formatter2);
        System.out.println("\nRan test at " + d);

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
                System.out.println("   -Holding " + coinSymbol + ": " + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%) [Paid: " + coinValuePaid + " Trail: " + df.format(trailingStopValue) + "] (" + trailingPercent + "%)");
                if(updateCycleCounter <1) {
                    Main.UPDATER.sendUpdateMsg("```(" + d + ") " + coinSymbol + ": " + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%)```");
                    updateCycleCounter = 60/Main.CYCLE_TIME; //only send discord updates every minute
                } else {
                    updateCycleCounter--;}
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
            BufferedReader br = new BufferedReader(new FileReader("c:\\Users\\Shaftspin\\Desktop\\IntelliJ\\IntelliJ IDEA Community Edition 2020.3.3\\AlgoTest\\allowed.txt"));
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

    private static String makeAPICall(String url) throws URISyntaxException, IOException {
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

    /*ALGO GOES HERE*/
    private String findNew(){
        return "DOGEUSD";
    }

    private void buyNew(String newTicker) {
        if(!newTicker.equals("")) {
            //BUY CODE GOES HERE
            if(tradeConfirm(newTicker)) {
                coinSymbol = newTicker;
                trailingPercent = trailingPercentBase;
                updateCurrent();
                trailingStopValue = (coinValue - ((trailingPercentBase / 100.0) * coinValue));
                updateCurrent();
                System.out.println("   -Bought " + coinSymbol + " at $" + coinValue + " [https://www.binance.us/en/trade/pro/" + coinSymbol + "]");
                Main.UPDATER.sendUpdateMsg("Bought " + coinSymbol + " at $" + coinValue + " [https://www.binance.us/en/trade/pro/" + coinSymbol + "]");
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
                    if(coinValuePaid == 0){
                        coinValuePaid = coinValue;}
                    coinPercentChange = ((100/ coinValuePaid) * coinValue)-100;
                    break;
                }
            }
            if (coinValue > coinValuePeak) {
                coinValuePeak = coinValue;
                trailingPercent =  trailingPercentBase;
                if(coinPercentChange>.6){                                             //at .6% gain, set trail down to .5%
                    trailingPercent = .5;
                }
                if(coinPercentChange>1) {                                             //trail stays at .5% until 1% gain
                    trailingPercent += (coinPercentChange - 1) * .1;                 //trail grows .1% for each addition 1% gain
                    if (trailingPercent > 3) {
                        trailingPercent = 3;
                    }
                }
                trailingStopValue = (coinValuePeak - ((trailingPercent / 100.0) * coinValuePeak));
            }
        } catch(IOException e){
            System.out.println("Error: cannot access content - " + e.toString());
        } catch(URISyntaxException e){
            System.out.println("Error: Invalid URL " + e.toString());
        }
    }

    public void sellCurrent() {
        //SELL CODE GOES HERE
        if(tradeConfirm("")) {
            System.out.println("   -Sold " + coinSymbol + " at $" + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%)");
            Main.UPDATER.sendUpdateMsg("Sold " + coinSymbol + " at $" + df.format(coinValue) + " (" + df.format(coinPercentChange) + "%)");

            String d = LocalDateTime.now().format(formatter);
            try {
                File file = new File("c:\\Users\\Shaftspin\\Desktop\\IntelliJ\\IntelliJ IDEA Community Edition 2020.3.3\\AlgoTest\\sellLog.txt");
                FileWriter fw = new FileWriter(file, true);
                if(file.length()!=0){fw.write("\n");}
                fw.write("(" + d + ") Sold " + coinSymbol + " (" + df.format(coinPercentChange) + "%)");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
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

    private boolean tradeConfirm(String s){
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
                    System.out.println(testTicker + " added to list.");
                }
            }
        } catch (IOException e) {
            System.out.println("Error: cannot access content - " + e.toString());
        } catch (URISyntaxException e) {
            System.out.println("Error: Invalid URL " + e.toString());
        }
        return;
    }*/
}