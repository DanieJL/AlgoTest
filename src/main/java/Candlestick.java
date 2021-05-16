public class Candlestick {
    private Long openTime;
    private double open;
    private double close;
    private double volume;
    private Long closeTime;

    public Candlestick(Long openTime, double open, double close, double volume, Long closeTime) {
        this.openTime = openTime;
        this.open = open;
        this.close = close;
        this.volume = volume;
        this.closeTime = closeTime;

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

    public Long getCloseTime() {
        return closeTime;
    }
}