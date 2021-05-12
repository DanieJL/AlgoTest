package util;

import java.util.Calendar;

public class GeneralUtil {
    public static long getDateDeltaUnix(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, days);

        return cal.getTime().getTime();
    }
}
