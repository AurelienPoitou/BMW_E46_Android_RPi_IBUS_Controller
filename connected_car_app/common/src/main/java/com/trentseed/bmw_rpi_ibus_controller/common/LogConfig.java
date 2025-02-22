package com.trentseed.bmw_rpi_ibus_controller.common;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogConfig {
    private static final Logger logger = Logger.getLogger(LogConfig.class.getName());

    public static void configure(File logDir) {
        try {
            // Create a timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
            // Create log file with timestamp
            File logFile = new File(logDir, "log_" + timestamp + ".txt");

            FileHandler fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            fileHandler.setLevel(Level.ALL);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
