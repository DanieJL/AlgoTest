package util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/*
 * Clean way of handling properties we want to keep hidden from Git
 * like api keys and discord tokens
 * Requires bot.config to be in the resources folder
 */
public class ConfigHandler {
    public static String getBotConfig(String config) {
        Properties properties = new Properties();
        String fileName = "/bot.config";

        InputStream is = ConfigHandler.class.getResourceAsStream(fileName);
        try {
            properties.load(is);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties.getProperty(config);
    }
}
