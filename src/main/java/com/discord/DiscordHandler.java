package com.discord;

import com.BotRunner;
import com.constants.Constants;
import com.market.MarketBot;
import com.util.ConfigHandler;
import com.util.GeneralUtil;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;

public class DiscordHandler extends ListenerAdapter {
    private TextChannel channel = null;
    private final static Logger LOGGER = Logger.getLogger(DiscordHandler.class);

    public DiscordHandler() {
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

        if (messageText.equals("!quit") || messageText.equals("!shutdown")) {
            handleQuitCommand();
        }
        if (messageText.equals("!pause")) {
            handlePauseCommand();
        }
        if (!BotRunner.getBusyMarket()) {
            BotRunner.MARKETBots.sort(Comparator.comparing(MarketBot::getAccountVal).reversed());
            if (messageText.contains("!sell")) {
                handleSellCommand(messageText);
            } else if (messageText.contains("!keeptop")) {
                handleKeepCommand(messageText);
            } else if (messageText.equals("!data")) {
                handleDataCommand();
            } else if (messageText.contains("!reset")) {
                handleResetCommand(messageText);
            } else if (messageText.contains("!mp")) {
                handleMpCommand(messageText);
            }
        } else {
            this.channel.sendMessage("Market bots are busy, try again.").queue();
        }
    }

    private void handleQuitCommand() {
        for (MarketBot marketBot : BotRunner.MARKETBots) {
            this.channel.sendMessage("Saving settings and shutting down [" + marketBot.getName() + "]").queue();
            marketBot.saveCurrentValues();
        }
        GeneralUtil.waitSeconds(5);
        System.exit(0);
    }

    private void handlePauseCommand() {
        if (!BotRunner.getBusyMarket() && !BotRunner.isBacktest()) {
            if (BotRunner.getPauseMarket()) {
                this.channel.sendMessage("Market unpaused.").queue();
                BotRunner.setPauseMarket(false);
            } else {
                this.channel.sendMessage("Market paused.").queue();
                BotRunner.setPauseMarket(true);
            }
        } else {
            this.channel.sendMessage("Please wait for current cycle to finish.").queue();
        }
    }

    private void handleKeepCommand(String messageText) {
        String[] cmdSplit = messageText.split(" ", 3);
        int top = 0;
        if (cmdSplit.length > 1) {
            try {
                top = Integer.parseInt(cmdSplit[1]);
            } catch (NumberFormatException e) {
                LOGGER.error(e);
            }
        }
        if (top == 0 || top > BotRunner.MARKETBots.size()) {
            return;
        }
        for (int i = 0; i < BotRunner.MARKETBots.size(); i++) {
            if (i > (top - 1)) {
                File file = new File("sellLogs", BotRunner.MARKETBots.get(i).getName() + "_sellLog.txt");
                if (file.delete()) {
                    this.channel.sendMessage("Deleted " + BotRunner.MARKETBots.get(i).getName()).queue();
                }
                else {
                    this.channel.sendMessage("Failed to delete " + BotRunner.MARKETBots.get(i).getName()).queue();
                }
            }
        }
    }

    private void handleSellCommand(String messageText) {
        for (MarketBot marketBot : BotRunner.MARKETBots) {
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
    }

    private void handleDataCommand() {
        for (MarketBot marketBot : BotRunner.MARKETBots) {
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
                    current = marketBot.getCoinSymbol() + " " + Constants.decimalFormat.format(marketBot.getCoinValue()) + " (" + Constants.decimalFormat.format(marketBot.getCoinPercentChange()) + "%)";
                }
                double percentGain = ((.1 * marketBot.getAccountVal()) - 100);
                String data = "Account Value: " + Constants.decimalFormat.format(marketBot.getAccountVal()) + " (" + Constants.decimalFormat.format(percentGain) + "%)\n"
                        + "Current: " + current + "\n\n"
                        + count + " trades: " + Constants.decimalFormat.format(total) + "%\n"
                        + "With " + Constants.feePercent + "% fee/trade: " + Constants.decimalFormat.format((total - (count * Constants.feePercent))) + "%\n"
                        + posCount + " positive trades: " + Constants.decimalFormat.format(posAvg) + "%/avg\n"
                        + negCount + " negative trades: " + Constants.decimalFormat.format(negAvg) + "%/avg";
                this.channel.sendMessage("```[" + marketBot.getName() + "]\n" + data + "```").queue();
            } catch (IOException e) {
                this.channel.sendMessage("```[" + marketBot.getName() + "] No sales completed by this bot.```").queue();
            }
        }
    }

    private void handleResetCommand(String messageText) {
        for (MarketBot marketBot : BotRunner.MARKETBots) {
            String[] cmdSplit = messageText.split(" ", 3);
            if (cmdSplit.length > 1 && (cmdSplit[1].equals(marketBot.getName()) || cmdSplit[1].equals("all"))) {
                marketBot.resetBot();
                this.channel.sendMessage("```[" + marketBot.getName() + "] Reset Successfully.```").queue();
                if (!cmdSplit[1].equals("all")) {
                    break;
                }
            }
        }
    }

    private void handleMpCommand(String messageText) {
        String[] MPcurrentBestName = new String[Constants.mpRanges.length + 1];                    //the current best performers in each range
        double[] MPcurrentBestValue = new double[Constants.mpRanges.length + 1];
        int[] MPcurrentBestTradeCount = new int[Constants.mpRanges.length + 1];
        String[] cmdSplit = messageText.split(" ", 3);
        for (MarketBot marketBot : BotRunner.MARKETBots) {
            if (cmdSplit.length > 1 && (cmdSplit[1].equals(marketBot.getName()) || cmdSplit[1].equals("all"))) {
                try {
                    File dir = new File("sellLogs");
                    dir.mkdir();
                    File file = new File(dir, marketBot.getName() + "_sellLog.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String st;
                    double[] coinPerformance = new double[Constants.mpRanges.length + 1];
                    int[] coinPerformanceTrades = new int[Constants.mpRanges.length + 1];
                    while ((st = br.readLine()) != null) {
                        double value = Double.parseDouble(st.substring(st.lastIndexOf("(") + 1, st.lastIndexOf("%")));
                        double mpValue = Double.parseDouble(st.substring(st.lastIndexOf("[") + 1, st.lastIndexOf("]")));

                        for (int i = 0; i < Constants.mpRanges.length; i++) {
                            if (i == 0) {
                                if (mpValue > Constants.mpRanges[i]) {
                                    coinPerformance[i] += value;
                                    coinPerformanceTrades[i]++;
                                }
                            } else if (i == Constants.mpRanges.length - 1) {
                                if ((mpValue >= Constants.mpRanges[i]) && (mpValue < Constants.mpRanges[i - 1])) {
                                    coinPerformance[i] += value;
                                    coinPerformanceTrades[i]++;
                                } else if (mpValue < Constants.mpRanges[i]) {
                                    coinPerformance[i + 1] += value;
                                    coinPerformanceTrades[i + 1]++;
                                }
                            } else if ((mpValue >= Constants.mpRanges[i]) && (mpValue < Constants.mpRanges[i - 1])) {
                                coinPerformance[i] += value;
                                coinPerformanceTrades[i]++;
                            }
                        }
                    }
                    for (int i = 0; i < coinPerformance.length; i++) { //this loop makes it go by avg instead of overall
                        coinPerformance[i] = coinPerformance[i] - (coinPerformanceTrades[i] * Constants.feePercent);
                    }
                    if (cmdSplit[1].equals("all")) {
                        for (int i = 0; i < Constants.mpRanges.length + 1; i++) {
                            if (coinPerformance[i] > MPcurrentBestValue[i]) {
                                MPcurrentBestName[i] = marketBot.getName();
                                MPcurrentBestValue[i] = coinPerformance[i];
                                MPcurrentBestTradeCount[i] = coinPerformanceTrades[i];
                            }
                        }
                    } else {
                        StringBuilder msg = new StringBuilder(marketBot.getName() + " market performance:\n```");
                        for (int i = 0; i < Constants.mpRanges.length; i++) {
                            if (i == 0) {
                                msg.append(">+").append(Constants.mpRanges[i]).append("%: [").append(marketBot.getName()).append("] ").append(coinPerformanceTrades[i]).append(" trades, ").append(Constants.decimalFormat.format(coinPerformance[i])).append("%\n");
                            } else {
                                msg.append(Constants.mpRanges[i]).append(" to ").append(Constants.mpRanges[i - 1]).append("%: [").append(marketBot.getName()).append("] ").append(coinPerformanceTrades[i]).append(" trades, ").append(Constants.decimalFormat.format(coinPerformance[i])).append("%\n");
                                if (i == Constants.mpRanges.length - 1) {
                                    msg.append("<").append(Constants.mpRanges[i]).append("%: [").append(marketBot.getName()).append("] ").append(coinPerformanceTrades[i + 1]).append(" trades, ").append(Constants.decimalFormat.format(coinPerformance[i + 1])).append("%\n");
                                }
                            }
                        }
                        msg.append("```");
                        this.channel.sendMessage(msg.toString()).queue();
                    }
                } catch (IOException e) {
                    LOGGER.error("Error reading sell log.");
                }
            }
        }
        if (messageText.equals("!mp all")) {
            StringBuilder msg = new StringBuilder("Overall market performance:\n```");
            for (int i = 0; i < Constants.mpRanges.length; i++) {
                if (i == 0) {
                    msg.append(">+").append(Constants.mpRanges[i])
                            .append("%: [").append(MPcurrentBestName[i]).append("] ")
                            .append(MPcurrentBestTradeCount[i]).append(" trades, ")
                            .append(Constants.decimalFormat.format(MPcurrentBestValue[i])).append("%\n");
                } else {
                    msg.append(Constants.mpRanges[i]).append(" to ").append(Constants.mpRanges[i - 1])
                            .append("%: [").append(MPcurrentBestName[i]).append("] ")
                            .append(MPcurrentBestTradeCount[i]).append(" trades, ")
                            .append(Constants.decimalFormat.format(MPcurrentBestValue[i])).append("%\n");
                    if (i == Constants.mpRanges.length - 1) {
                        msg.append("<").append(Constants.mpRanges[i]).append("%: [").append(MPcurrentBestName[i + 1]).append("] ")
                                .append(MPcurrentBestTradeCount[i + 1]).append(" trades, ")
                                .append(Constants.decimalFormat.format(MPcurrentBestValue[i + 1])).append("%\n");
                    }
                }
            }
            msg.append("```{");
            for (int i = 0; i < MPcurrentBestName.length; i++) {
                msg.append("\"").append(MPcurrentBestName[i]).append("\"");
                if (i != MPcurrentBestName.length - 1) {
                    msg.append(", ");
                }
            }
            msg.append("};");
            this.channel.sendMessage(msg.toString()).queue();
        }
    }

    public void sendUpdateMsg(String msg) {
        this.channel.sendMessage(msg).queue();
    }
}