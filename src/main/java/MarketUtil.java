import enums.KlineInterval;
import org.json.JSONArray;
import util.ApiClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class MarketUtil {
    public static String[] allowedTickers;
    private final ApiClient apiClient;

    public MarketUtil() {
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

    private double calcSmmaUp(List<Candlestick> candlesticks, double n, int i, double avgUt1){

        if(avgUt1==0){
            double sumUpChanges = 0;

            for(int j = 0; j < n; j++){
                double change = candlesticks.get(i-j).getClose() - candlesticks.get(i-j).getOpen();

                if(change > 0){
                    sumUpChanges+= change;
                }
            }
            return sumUpChanges / n;
        }else {
            double change = candlesticks.get(i).getClose() - candlesticks.get(i).getOpen();
            if(change < 0){
                change = 0;
            }
            return ((avgUt1 * (n-1)) + change) / n ;
        }

    }

    private double calcSmmaDown(List<Candlestick> candlesticks, double n, int i, double avgDt1){
        if(avgDt1==0){
            double sumDownChanges = 0;

            for(int j = 0; j < n; j++){
                double change = candlesticks.get(i - j).getClose() - candlesticks.get(i - j).getOpen();

                if(change < 0){
                    sumDownChanges-= change;
                }
            }
            return sumDownChanges / n;
        }else {
            double change = candlesticks.get(i).getClose() - candlesticks.get(i).getOpen();
            if(change > 0){
                change = 0;
            }
            return ((avgDt1 * (n-1)) - change) / n ;
        }
    }

    public double[] calculateRSIValues(List<Candlestick> candlesticks, int n){
        double[] results = new double[candlesticks.size()];
        double ut1 = 0;
        double dt1 = 0;
        for(int i = 0; i < candlesticks.size(); i++){
            if(i<(n)){
                continue;
            }

            ut1 = calcSmmaUp(candlesticks, n, i, ut1);
            dt1 = calcSmmaDown(candlesticks, n, i, dt1);

            results[i] = 100.0 - 100.0 / (1.0 + (ut1 / dt1));
        }
        return results;
    }

    public double[] calculateKeyFibRetracements(List<Candlestick> candlesticks) {
        Candlestick min = candlesticks.stream().min(Comparator.comparing(Candlestick::getClose)).get();
        double max;
        Optional<Candlestick> maxOptional = candlesticks.subList(candlesticks.indexOf(min), candlesticks.size() - 1)
                .stream()
                .max(Comparator.comparing(Candlestick::getClose));

        if (maxOptional.isPresent() && (candlesticks.indexOf(maxOptional.get()) - candlesticks.indexOf(min) > 10)) {
            max = maxOptional.get().getClose();
        } else {
            max = 99999999;
        }

        double diff = max - min.getClose();

        double level382 = getIndFibRetracement(max, diff, .382);
        double level5 = getIndFibRetracement(max, diff, .5);
        double level618 = getIndFibRetracement(max, diff, .618);

        return new double[]{level382, level5, level618};
    }

    private double getIndFibRetracement(double max, double diff, double level) {
        return max - (diff * level);
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

    public Map<String, List<Candlestick>> getKlineForAllTickers(KlineInterval interval) {
        Map<String, List<Candlestick>> klineMap = new HashMap<>();
        for (String ticker : allowedTickers) {
            List<Candlestick> klines = getKlineData(ticker, interval.getInterval());
            klineMap.put(ticker, klines);
        }
        return klineMap;
    }
}
