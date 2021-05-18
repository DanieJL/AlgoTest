package com.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
