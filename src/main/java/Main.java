import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static final int CYCLE_TIME = 15;     //run the market test every X seconds
    public static DiscordBot UPDATER = new DiscordBot();
    public static Market MOGUL = new Market();

    public static void main(String[] args) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                MOGUL.MarketBot();
            }
        }, 0, 1000L * CYCLE_TIME); //60000 * MINUTES

    }
}

//TODO: stuff
//Store BOT key/channel ID/files locations/binance key more smarterly
//Implement actual trading
//Implement tradeConfirm()
//Put into GIT
//Case do janitor work and clean up this stupid fucking retarded program

//**ALGO STUFF**
//RSI seems off?
//Scale acceptable buys based on market conditions, positive tend = slightly looser reqs
//Block things that had any major jumps in price?
//Implement sentiment scalping?
//Filter for more recent volume requirements?
//Limit gains per day? (stop trading after x% gain (or loss) on a day?)
