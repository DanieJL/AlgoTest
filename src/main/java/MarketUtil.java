import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class MarketUtil {
    private static double calcSmmaUp(List<Candlestick> candlesticks, double n, int i, double avgUt1){

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

    private static double calcSmmaDown(List<Candlestick> candlesticks, double n, int i, double avgDt1){
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

    public static double[] calculateRSIValues(List<Candlestick> candlesticks, int n){
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

    public static double[] calculateKeyFibRetracements(List<Candlestick> candlesticks) {
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

    private static double getIndFibRetracement(double max, double diff, double level) {
        return max - (diff * level);
    }
}
