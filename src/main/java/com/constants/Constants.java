package com.constants;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class Constants {
    public static final int UPDATE_CYCLE_TIME = 2;
    public static final DateTimeFormatter longDateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
    public static final DecimalFormat decimalFormat = new DecimalFormat("#.###");
    public static final DateTimeFormatter shortDateFormat = DateTimeFormatter.ofPattern("hh:mm a");
    public static final int CYCLE_TIME = 15;         //run the com.market test every X seconds (when live)
    public static final double feePercent = .15;     //estimated total fee as a % - per transactions (both buy/sell and spread)
    public static final double[] mpRanges = {11, 8, 5, 3, 2, 1, 0, -1, -2, -3, -5, -8, -11}; //needs to be ordered greater to lesser
    public static final int mpCalcRange = 120;      //how many klines to use to calculate com.market performance
    public static final String botListFileName = "src/main/resources/BotList.json";
    public static final String[] godBotAlgos = {"null", "Fib618ANDRSILT30", "RSILT30CheckOnly", "Fib618CheckOnly", "Fib618CheckOnly", "RSILT30CheckOnly", "null", "Fib618ANDRSILT30", "Fib618CheckOnly", "null", "null", "null", "Fib618ANDRSILT30", "Fib618ANDRSILT30"};

}
