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

public class DiscordBot extends ListenerAdapter {
    private TextChannel channel = null;

    private final static Logger LOGGER = Logger.getLogger(DiscordBot.class);

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
        if (event.getAuthor().isBot()) {return;}

        String messageText = event.getMessage().getContentRaw();
        if (!event.getChannel().getId().equals(channel.getId()))
            return;

        if (messageText.equals("!quit") || messageText.equals("!shutdown")) {
            for (Market market : Main.MARKETS) {
                this.channel.sendMessage("Saving settings and shutting down [" + market.getName() + "]").queue();
                market.saveCurrentValues();
            }
            GeneralUtil.waitSeconds(5);
            System.exit(0);
        }
        for (Market market : Main.MARKETS) {
            if (messageText.equals("!ping")) {
                event.getChannel().sendMessage("PONG BITCH!").queue();
                if (market.getCoinSymbol().equals("")) {
                    this.channel.sendMessage(market.getName() + " is currently looking for a coin!").queue();
                }
            }
            if (messageText.contains("!sell")) {
                String cmdSplit[] = messageText.split(" ", 3);
                if (cmdSplit.length > 1 && cmdSplit[1].equals(market.getName())) {
                    if (market.getCoinSymbol().equals("")) {
                        this.channel.sendMessage(market.getName() + " isn't holding anything to sell!").queue();
                    } else {
                        market.updateCurrent();
                        market.sellCurrent();
                    }
                    break;
                } else if (cmdSplit.length > 1) {
                    continue;
                }
                if (market.getCoinSymbol().equals("")) {
                    this.channel.sendMessage(market.getName() + " isn't holding anything to sell!").queue();
                } else {
                    market.updateCurrent();
                    market.sellCurrent();
                }
            }
            if (messageText.equals("!data")) {
                try {
                    File dir = new File("sellLogs");
                    dir.mkdir();
                    File file = new File(dir,market.getName() + "_sellLog.txt");
                    BufferedReader br = new BufferedReader(new FileReader(file));
                    String st;
                    int posCount = 0;
                    double posAvg = 0;
                    int negCount = 0;
                    double negAvg = 0;
                    double total = 0;
                    int count = 0;
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
                    String data = count + " trades: " + total + "%\nAdjusted: " + (total - ((count) * .15)) + "%\n\n" + //adjusted is assuming .075%x2 fee to buy and sell
                            posCount + " positive trades: " + posAvg + "%/avg\n" + negCount + " negative trades: " + negAvg + "%/avg\nAccount Value: " + market.getAccountVal() + "\n\n";
                    this.channel.sendMessage("```[" + market.getName() + "]\n" + data + "```").queue();
                } catch (IOException e) {
                    this.channel.sendMessage("```[" + market.getName() + "] No sales completed by this bot.```").queue();
                }
            }
        }
    }

    public void sendUpdateMsg(String msg){
        this.channel.sendMessage(msg).queue();
    }
}