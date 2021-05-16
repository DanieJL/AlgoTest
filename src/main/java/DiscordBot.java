import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.log4j.Logger;
import util.ConfigHandler;
import util.GeneralUtil;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Comparator;

public class DiscordBot extends ListenerAdapter {
    private TextChannel channel = null;
    private final static Logger LOGGER = Logger.getLogger(DiscordBot.class);
    private static final DecimalFormat df = new DecimalFormat("#.###");


    DiscordBot() {
        JDABuilder BOT = JDABuilder.createDefault(ConfigHandler.getBotConfig("discord.token"));
        BOT.addEventListeners(this);
        try {
            channel = BOT.build().
                    awaitReady().
                    getTextChannelById(ConfigHandler.getBotConfig("discord.channel.id"));
            LOGGER.info("Successfully logged into Discord.");
        } catch (LoginException | InterruptedException e) {
            LOGGER.error("UNABLE TO LOGIN.");
        }
    }

    /*WILL NEED TO MAKE SURE COMMANDS CANNOT INTERRUPT THINGS THAT DESYNC BOT (Like starting a sell while it's already trying to confirm a sell)*/
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.getChannel().getId().equals(channel.getId())) {
            return;
        }

        String messageText = event.getMessage().getContentRaw();
        if (!event.getChannel().getId().equals(channel.getId()))
            return;

        if (messageText.equals("!quit") || messageText.equals("!shutdown")) {
            for (MarketBot marketBot : Main.MARKETBots) {
                this.channel.sendMessage("Saving settings and shutting down [" + marketBot.getName() + "]").queue();
                marketBot.saveCurrentValues();
            }
            GeneralUtil.waitSeconds(5);
            System.exit(0);
        }
        if (!Main.getBusyMarket()) { //make sure these commands cannot interfere with bot, simultaneous stuff causes crash
            Main.MARKETBots.sort(Comparator.comparing(MarketBot::getAccountVal).reversed());
            String[] MPcurrentBestName = new String[MarketBot.mpRanges.length];                    //the current best performers in each range
            double[] MPcurrentBestValue = new double[MarketBot.mpRanges.length];
            int[] MPcurrentBestTradeCount = new int[MarketBot.mpRanges.length];
            for (MarketBot marketBot : Main.MARKETBots) {
                if (messageText.contains("!sell")) {
                    String[] cmdSplit = messageText.split(" ", 3);
                    if (cmdSplit.length > 1 && (cmdSplit[1].equals(marketBot.getName()) || cmdSplit[1].equals("all"))) {
                        if (marketBot.getCoinSymbol().equals("")) {
                            this.channel.sendMessage(marketBot.getName() + " isn't holding anything to sell!").queue();
                        } else {
                            marketBot.updateCurrent();
                            marketBot.sellCurrent();
                        }
                        if (!cmdSplit[1].equals("all")) {
                            break;
                        }
                    }
                }
                if (messageText.equals("!data")) {
                    try {
                        File dir = new File("sellLogs");
                        dir.mkdir();
                        File file = new File(dir, marketBot.getName() + "_sellLog.txt");
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        String st;
                        int posCount = 0;
                        double posAvg = 0;
                        int negCount = 0;
                        double negAvg = 0;
                        double total = 0;
                        double count = 0;
                        while ((st = br.readLine()) != null) {
                            double value = Double.parseDouble(st.substring(st.lastIndexOf("(") + 1, st.lastIndexOf("%")));
                            total += value;
                            if (value > 0) {
                                posCount++;
                                posAvg += value;
                            } else {
                                negCount++;
                                negAvg += value;
                            }
                            count++;
                        }
                        posAvg = posAvg / posCount;
                        negAvg = negAvg / negCount;
                        String current = "Searching...";
                        if (!marketBot.getCoinSymbol().isEmpty()) {
                            count += .5;
                            current = marketBot.getCoinSymbol() + " " + df.format(marketBot.getCoinValue()) + " (" + df.format(marketBot.getCoinPercentChange()) + "%)";
                        }
                        double percentGain = ((.1 * marketBot.getAccountVal()) - 100);
                        String data = "Account Value: " + df.format(marketBot.getAccountVal()) + " (" + df.format(percentGain) + "%)\n"
                                + "Current: " + current + "\n\n"
                                + count + " trades: " + df.format(total) + "%\n"
                                + "With " + Main.feePercent + "% fee/trade: " + df.format((total - (count * Main.feePercent))) + "%\n"
                                + posCount + " positive trades: " + df.format(posAvg) + "%/avg\n"
                                + negCount + " negative trades: " + df.format(negAvg) + "%/avg";
                        this.channel.sendMessage("```[" + marketBot.getName() + "]\n" + data + "```").queue();
                    } catch (IOException e) {
                        this.channel.sendMessage("```[" + marketBot.getName() + "] No sales completed by this bot.```").queue();
                    }
                }
                if (messageText.contains("!reset")) {
                    String[] cmdSplit = messageText.split(" ", 3);
                    if (cmdSplit.length > 1 && (cmdSplit[1].equals(marketBot.getName()) || cmdSplit[1].equals("all"))) {
                        marketBot.resetBot();
                        this.channel.sendMessage("```[" + marketBot.getName() + "] Reset Successfully.```").queue();
                        if (!cmdSplit[1].equals("all")) {
                            break;
                        }
                    }
                }

                if (messageText.contains("!mp")) {
                    String[] cmdSplit = messageText.split(" ", 3);
                    if (cmdSplit.length > 1 && (cmdSplit[1].equals(marketBot.getName()) || cmdSplit[1].equals("all"))) {
                        try {
                            File dir = new File("sellLogs");
                            dir.mkdir();
                            File file = new File(dir, marketBot.getName() + "_sellLog.txt");
                            BufferedReader br = new BufferedReader(new FileReader(file));
                            String st;
                            double[] coinPerformance = new double[MarketBot.mpRanges.length];
                            int[] coinPerformanceTrades = new int[MarketBot.mpRanges.length];
                            while ((st = br.readLine()) != null) {
                                double value = Double.parseDouble(st.substring(st.lastIndexOf("(") + 1, st.lastIndexOf("%")));
                                double mpValue = Double.parseDouble(st.substring(st.lastIndexOf("[") + 1, st.lastIndexOf("]")));

                                for (int i = 0; i < MarketBot.mpRanges.length; i++) {
                                    if (mpValue < MarketBot.mpRanges[i]) {
                                        continue;
                                    }
                                    coinPerformance[i] += value;
                                    coinPerformanceTrades[i] += 1;
                                    break;
                                }
                            }
                            for (int i = 0; i < MarketBot.mpRanges.length; i++) { //this loop makes it go by avg instead of overall
                                coinPerformance[i] = coinPerformance[i] / coinPerformanceTrades[i];
                            }
                            if (cmdSplit[1].equals("all")) {
                                for (int i = 0; i < MarketBot.mpRanges.length; i++) {
                                    if (coinPerformance[i] > MPcurrentBestValue[i]) {
                                        MPcurrentBestValue[i] = coinPerformance[i];
                                        MPcurrentBestTradeCount[i] = coinPerformanceTrades[i];
                                        MPcurrentBestName[i] = marketBot.getName();
                                    }
                                }
                            } else {
                                String msg = marketBot.getName() + " market performance:\n```";
                                String plus = "+";
                                for (int i = 0; i < MarketBot.mpRanges.length; i++) {
                                    if (MarketBot.mpRanges[i] <= 0) {
                                        plus = "";
                                        if (MarketBot.mpRanges[i] == 0) {
                                            plus = " ";
                                        }
                                    }
                                    msg += plus + MarketBot.mpRanges[i] + "%: [" + marketBot.getName() + "] (" + coinPerformanceTrades[i] + " trades) " + df.format(coinPerformance[i]) + "%avg\n";
                                }
                                msg += "```";
                                this.channel.sendMessage(msg).queue();
                            }
                        } catch (IOException e) {
                            LOGGER.error("Error reading sell log.");
                        }
                    }
                }
            }
            if (messageText.equals("!mp all")) {
                String msg = "Overall market performance: (" + df.format(Main.getMarketPerformance()) + "%)\n```";
                String plus = "+";
                for (int i = 0; i < MarketBot.mpRanges.length; i++) {
                    if (MarketBot.mpRanges[i] <= 0) {
                        plus = "";
                        if(MarketBot.mpRanges[i]==0) {
                            plus = " ";
                        }
                    }
                    if (MarketBot.mpRanges[i] == 0) {
                        plus = " ";
                    }
                    msg += plus + MarketBot.mpRanges[i] + "%: [" + MPcurrentBestName[i] + "] (" + MPcurrentBestTradeCount[i] + " trades) " + df.format(MPcurrentBestValue[i]) + "%avg\n";
                }
                msg += "```";
                this.channel.sendMessage(msg).queue();
            }
        } else {
            this.channel.sendMessage("Market bots are busy, try again.").queue();
        }
    }

    public void sendUpdateMsg(String msg) {
        this.channel.sendMessage(msg).queue();
    }
}