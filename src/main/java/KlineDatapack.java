import java.util.List;
import java.util.Map;

/**
 * Doing a class like this in case in the future we want to grab multiple interval sets
 * like 4h and 1d or anything like that. This future proofs being able to do that
 * and the algorithms are already setup to consume this "datapack"
 */
public class KlineDatapack {

    private Map<String, List<Candlestick>> kline1mData;
    private Map<String, List<Candlestick>> kline3mData;
    private Map<String, List<Candlestick>> kline5mData;
    private Map<String, List<Candlestick>> kline15mData;
    private Map<String, List<Candlestick>> kline30mData;
    private Map<String, List<Candlestick>> kline1hData;
    private Map<String, List<Candlestick>> kline2hData;
    private Map<String, List<Candlestick>> kline4hData;
    private Map<String, List<Candlestick>> kline6hData;
    private Map<String, List<Candlestick>> kline8hData;
    private Map<String, List<Candlestick>> kline12hData;
    private Map<String, List<Candlestick>> kline1dData;
    private Map<String, List<Candlestick>> kline3dData;
    private Map<String, List<Candlestick>> kline1MData;

    public Map<String, List<Candlestick>> getKline1mData() {
        return kline1mData;
    }

    public void setKline1mData(Map<String, List<Candlestick>> kline1mData) {
        this.kline1mData = kline1mData;
    }

    public Map<String, List<Candlestick>> getKline3mData() {
        return kline3mData;
    }

    public void setKline3mData(Map<String, List<Candlestick>> kline3mData) {
        this.kline3mData = kline3mData;
    }

    public Map<String, List<Candlestick>> getKline5mData() {
        return kline5mData;
    }

    public void setKline5mData(Map<String, List<Candlestick>> kline5mData) {
        this.kline5mData = kline5mData;
    }

    public Map<String, List<Candlestick>> getKline15mData() {
        return kline15mData;
    }

    public void setKline15mData(Map<String, List<Candlestick>> kline15mData) {
        this.kline15mData = kline15mData;
    }

    public Map<String, List<Candlestick>> getKline30mData() {
        return kline30mData;
    }

    public void setKline30mData(Map<String, List<Candlestick>> kline30mData) {
        this.kline30mData = kline30mData;
    }

    public Map<String, List<Candlestick>> getKline1hData() {
        return kline1hData;
    }

    public void setKline1hData(Map<String, List<Candlestick>> kline1hData) {
        this.kline1hData = kline1hData;
    }

    public Map<String, List<Candlestick>> getKline2hData() {
        return kline2hData;
    }

    public void setKline2hData(Map<String, List<Candlestick>> kline2hData) {
        this.kline2hData = kline2hData;
    }

    public Map<String, List<Candlestick>> getKline4hData() {
        return kline4hData;
    }

    public void setKline4hData(Map<String, List<Candlestick>> kline4hData) {
        this.kline4hData = kline4hData;
    }

    public Map<String, List<Candlestick>> getKline6hData() {
        return kline6hData;
    }

    public void setKline6hData(Map<String, List<Candlestick>> kline6hData) {
        this.kline6hData = kline6hData;
    }

    public Map<String, List<Candlestick>> getKline8hData() {
        return kline8hData;
    }

    public void setKline8hData(Map<String, List<Candlestick>> kline8hData) {
        this.kline8hData = kline8hData;
    }

    public Map<String, List<Candlestick>> getKline12hData() {
        return kline12hData;
    }

    public void setKline12hData(Map<String, List<Candlestick>> kline12hData) {
        this.kline12hData = kline12hData;
    }

    public Map<String, List<Candlestick>> getKline1dData() {
        return kline1dData;
    }

    public void setKline1dData(Map<String, List<Candlestick>> kline1dData) {
        this.kline1dData = kline1dData;
    }

    public Map<String, List<Candlestick>> getKline3dData() {
        return kline3dData;
    }

    public void setKline3dData(Map<String, List<Candlestick>> kline3dData) {
        this.kline3dData = kline3dData;
    }

    public Map<String, List<Candlestick>> getKline1MData() {
        return kline1MData;
    }

    public void setKline1MData(Map<String, List<Candlestick>> kline1MData) {
        this.kline1MData = kline1MData;
    }

}
