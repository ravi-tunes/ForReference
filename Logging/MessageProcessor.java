package com.example.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageProcessor {
    private static final Logger logger = LogManager.getLogger(MessageProcessor.class);

    public void processMessage(String message, boolean filterOut) {
        if (filterOut) {
            logger.log(CustomLogLevels.FILTERED, "Filtered message: {}", message);
        } else {
            logger.info("Processing message: {}", message);
        }
    }
}