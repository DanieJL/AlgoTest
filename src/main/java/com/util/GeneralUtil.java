package com.util;

import org.apache.log4j.Logger;

import java.util.Calendar;

public class GeneralUtil {
    private final static Logger LOGGER = Logger.getLogger(GeneralUtil.class);

    public static long getDateDeltaUnix(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);

        return cal.getTime().getTime();
    }

    public static void waitSeconds(long seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {
            LOGGER.error("Error waiting." + e.getMessage());
        }
    }
}
