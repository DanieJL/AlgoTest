package com.market;

import com.BotRunner;
import com.constants.Constants;
import com.enums.KlineInterval;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Demo {
    private final static Logger LOGGER = Logger.getLogger(BotRunner.class);
    private int dataLength = 30;
    private int testLength = 7;
    private int testStartDaysBack = 10;
    private int numberOfTests = 3;

    private String[] topDefaultAlgos;
    private String[] top5optimizedAlgos;
    private String[] top3optimizedAlgos;

    public Demo(){
        for(int i = 0; i<numberOfTests;i++){
            int increment = testLength*i;
            String dateFileName = "D" + i;
            new MarketDataHandler().generateBacktestDataFile(dateFileName, (testStartDaysBack-dataLength)-increment, (testStartDaysBack)-increment, KlineInterval.ONE_MINUTE);
            String testFileName = "T" + i;
            new MarketDataHandler().generateBacktestDataFile(testFileName, (testStartDaysBack)-increment, (testStartDaysBack+testLength)-increment, KlineInterval.ONE_MINUTE);

            BotRunner.backtestRun(dateFileName);
            topDefaultAlgos = getGodAlgos();
            getTopXAlgos(5);
            top5optimizedAlgos = getGodAlgos();
            getTopXAlgos(3);
            top3optimizedAlgos = getGodAlgos();

            Constants.godBotAlgos = topDefaultAlgos;
            BotRunner.backtestRun(testFileName);
            for(MarketBot m : BotRunner.MARKETBots){
                if(m.getName().equals("godBot")){
                    BotRunner.UPDATER.sendUpdateMsg("godBot test "+ i +" (default): " + (((m.getAccountVal()/1000)*100)-100) + "%");
                }
                m.resetBot();
            }
            Constants.godBotAlgos = top5optimizedAlgos;
            BotRunner.backtestRun(testFileName);
            for(MarketBot m : BotRunner.MARKETBots){
                if(m.getName().equals("godBot")){
                    BotRunner.UPDATER.sendUpdateMsg("godBot test "+ i +" (top 5): " + (((m.getAccountVal()/1000)*100)-100) + "%");
                }
                m.resetBot();
            }
            Constants.godBotAlgos = top3optimizedAlgos;
            BotRunner.backtestRun(testFileName);
            for(MarketBot m : BotRunner.MARKETBots){
                if(m.getName().equals("godBot")){
                    BotRunner.UPDATER.sendUpdateMsg("godBot test "+ i +" (top 3): " + (((m.getAccountVal()/1000)*100)-100) + "%");
                }
            }
        }
    }


    private String[] getGodAlgos() {
        String[] MPcurrentBestName = new String[Constants.mpRanges.length + 1];                    //the current best performers in each range
        double[] MPcurrentBestValue = new double[Constants.mpRanges.length + 1];
        int[] MPcurrentBestTradeCount = new int[Constants.mpRanges.length + 1];
        for (MarketBot marketBot : BotRunner.MARKETBots) {
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
        BotRunner.UPDATER.sendUpdateMsg(msg.toString());
        return MPcurrentBestName;
    }

    private void getTopXAlgos(int howMany){
        if (howMany == 0 || howMany > BotRunner.MARKETBots.size()) {
            return;
        }
        for (int i = 0; i < BotRunner.MARKETBots.size(); i++) {
            if (i > (howMany - 1)) {
                File file = new File("sellLogs", BotRunner.MARKETBots.get(i).getName() + "_sellLog.txt");
                file.delete();
            }
        }
        BotRunner.UPDATER.sendUpdateMsg("Deleted all but top " + howMany + ".");
    }
}
