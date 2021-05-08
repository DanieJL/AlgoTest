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

public class DiscordBot extends ListenerAdapter {
    private TextChannel channel;

    private final static Logger LOGGER = Logger.getLogger(DiscordBot.class);

    DiscordBot() {
        JDABuilder BOT = JDABuilder.createDefault("ODM5NzMyMDYwNDQ0NDI2MjUw.YJN7bA.ERRfPU_OyXFjqW3DXuUB0I0Gj5o");
        BOT.addEventListeners(this);
        try {
            channel = BOT.build().
                    awaitReady().
                    getTextChannelById("827394686065836076");
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

        if(messageText.equals("!ping")){
            event.getChannel().sendMessage("PONG BITCH!").queue();
            if(Main.MOGUL.getCoinSymbol().equals("")){
                event.getChannel().sendMessage("Currently looking for a coin!").queue();
            }
        }
        if(messageText.equals("!quit") || messageText.equals("!shutdown") ){
            event.getChannel().sendMessage("SHUTTING DOWN").queue();
            Main.MOGUL.saveCurrentValues();
            System.exit(0);
        }
        if(messageText.equals("!sell")){
            if(Main.MOGUL.getCoinSymbol().equals("")){
                event.getChannel().sendMessage("You aren't holding anything to sell!").queue();
            }
            else{
                Main.MOGUL.updateCurrent();
                Main.MOGUL.sellCurrent();
            }
        }
        if(messageText.equals("!data")) {
            File file = new File("sellLog.txt");
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String st;
                int posCount = 0; double posAvg = 0;
                int negCount = 0; double negAvg = 0;
                double total = 0;
                int count = 0;
                while ((st = br.readLine()) != null){
                    double value = Double.parseDouble(st.substring(st.lastIndexOf("(")+1,st.lastIndexOf("%")));
                    total += value;
                    if(value>0){posCount++; posAvg+=value;}
                    else{negCount++; negAvg+=value;}
                    count++;
                }
                posAvg = posAvg/posCount;
                negAvg = negAvg/negCount;
                String data = count + " trades: " + total + "%\nAdjusted: " + (total-((count)*.2)) + "%\n\n" +
                        posCount + " positive trades: " + posAvg + "%/avg\n" + negCount + " negative trades: " + negAvg + "%/avg";
                event.getChannel().sendMessage(data).queue();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendUpdateMsg(String msg){
        this.channel.sendMessage(msg).queue();
    }
}