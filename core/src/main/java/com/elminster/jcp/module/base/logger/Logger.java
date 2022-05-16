package com.elminster.jcp.module.base.logger;

import org.slf4j.LoggerFactory;

public class Logger {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void log(Object object) {
        logger.info(String.valueOf(object));
    }
}
