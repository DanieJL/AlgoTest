public class Candlestick {
    private Long openTime;
    private double open;
    private double close;
    private double volume;

    public Candlestick(Long openTime, double open, double close, double volume) {
        this.openTime = openTime;
        this.open = open;
        this.close = close;
        this.volume = volume;

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

    public double getVolume() {
        return volume;
    }
}