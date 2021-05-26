package com.market;

import com.BotRunner;
import com.constants.Constants;
import com.util.GeneralUtil;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import static com.BotRunner.MARKETBots;

public class Demo {
    private final static Logger LOGGER = Logger.getLogger(BotRunner.class);
    private static final int numberOfTests = 3;
    private static final int dataLength = 30;
    private static final int testLength = 7;
    private static final int startDaysBack = 10;

    public static int testStartDaysBack = 0;
    public static int testEndDaysBack = 0;



    public Demo(){
        BotRunner.UPDATER.sendUpdateMsg("<<DEMO BACKTEST STARTED FOR " + numberOfTests + " CYCLES>>");
        BotRunner.UPDATER.sendUpdateMsg("Using " + dataLength + " days of data to test following " + testLength+ " days.");
        double[] topDefaultPerf = new double[numberOfTests];

        for(int i = 0; i<numberOfTests;i++){
            int increment = i*testLength;
            testStartDaysBack = startDaysBack + increment + dataLength;
            testEndDaysBack = startDaysBack + increment;
            BotRunner.backtestRun();
            GeneralUtil.waitSeconds(300);
            Constants.godBotAlgos = getGodAlgos();
            //BotRunner.UPDATER.sendUpdateMsg(Arrays.toString(Constants.godBotAlgos));
            testStartDaysBack = startDaysBack + increment;
            testEndDaysBack = startDaysBack + increment - testLength;
            BotRunner.backtestRun();
            GeneralUtil.waitSeconds(60);
            for(MarketBot m : MARKETBots){
                if(m.getName().equals("godBot")){
                    topDefaultPerf[i] = ((m.getAccountVal()*.1)-100);
                    BotRunner.UPDATER.sendUpdateMsg("[CYCLE "+ (i+1) + "] Default: " + Double.toString(topDefaultPerf[i]) + "%");
                }
                m.resetBot();
            }
        }
        double defAvg = 0;
        for(int i = 0; i<topDefaultPerf.length;i++) {defAvg += topDefaultPerf[i]; defAvg = defAvg/topDefaultPerf.length;}
        BotRunner.UPDATER.sendUpdateMsg("<<DEMO BACKTEST COMPLETE>>");
        BotRunner.UPDATER.sendUpdateMsg("Default godBot: ("+ defAvg +"/avg) " + Arrays.toString(topDefaultPerf));
    }


    private String[] getGodAlgos() {
        String[] MPcurrentBestName = new String[Constants.mpRanges.length + 1];                    //the current best performers in each range
        double[] MPcurrentBestValue = new double[Constants.mpRanges.length + 1];
        int[] MPcurrentBestTradeCount = new int[Constants.mpRanges.length + 1];
        for (MarketBot marketBot : MARKETBots) {
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
                for (int i = 0; i < Constants.mpRanges.length + 1; i++) {
                    if (coinPerformance[i] > MPcurrentBestValue[i]) {
                        MPcurrentBestName[i] = marketBot.getName();
                        MPcurrentBestValue[i] = coinPerformance[i];
                        MPcurrentBestTradeCount[i] = coinPerformanceTrades[i];
                    }
                }
                br.close();
            } catch (IOException e) {
                LOGGER.error("Error reading sell log.");
            }
        }
/*        StringBuilder msg = new StringBuilder("Overall market performance:\n```");
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
        BotRunner.UPDATER.sendUpdateMsg(msg.toString());*/
        return MPcurrentBestName;
    }

    private void getTopXAlgos(int howMany){
        BotRunner.MARKETBots.sort(Comparator.comparing(MarketBot::getAccountVal).reversed());
        if (howMany == 0 || howMany > MARKETBots.size()) {
            return;
        }
        for (int i = 0; i < MARKETBots.size(); i++) {
            if (i > (howMany - 1)) {
                File file = new File("sellLogs", MARKETBots.get(i).getName() + "_sellLog.txt");
                file.delete();
            }
        }
        //BotRunner.UPDATER.sendUpdateMsg("Deleted all but top " + howMany + ".");
    }
}
