public class Candlestick {
    private Long openTime;
    private double open;
    private double close;

    public Candlestick(Long openTime, double open, double close) {
        this.openTime = openTime;
        this.open = open;
        this.close = close;

    }

    public Long getOpenTime() {
        return openTime;
    }

    public double getOpen() {
        return open;
    }

    public double getClose() {
        return close;
    }
}