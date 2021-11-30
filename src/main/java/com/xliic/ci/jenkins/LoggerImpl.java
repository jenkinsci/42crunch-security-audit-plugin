package com.xliic.ci.jenkins;

import java.io.PrintStream;
import com.xliic.cicd.audit.Logger;

public class LoggerImpl implements Logger {
    private PrintStream logger;
    private int level;

    LoggerImpl(final PrintStream logger, String logLevel) {
        this.logger = logger;
        switch (logLevel.toUpperCase()) {
            case "FATAL":
                this.level = Logger.Level.FATAL;
                break;
            case "ERROR":
                this.level = Logger.Level.ERROR;
                break;
            case "WARN":
                this.level = Logger.Level.WARN;
                break;
            case "INFO":
                this.level = Logger.Level.INFO;
                break;
            case "DEBUG":
                this.level = Logger.Level.DEBUG;
                break;
            default:
                logger.println("Unknown log level specified, setting log level to INFO");
                this.level = Logger.Level.INFO;
        }
    }

    @Override
    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void fatal(String message) {
        if (Logger.Level.FATAL >= level) {
            logger.println(message);
        }
    }

    @Override
    public void error(String message) {
        if (Logger.Level.ERROR >= level) {
            logger.println(message);
        }
    }

    @Override
    public void warn(String message) {
        if (Logger.Level.WARN >= level) {
            logger.println(message);
        }
    }

    @Override
    public void info(String message) {
        if (Logger.Level.INFO >= level) {
            logger.println(message);
        }
    }

    @Override
    public void debug(String message) {
        if (Logger.Level.DEBUG >= level) {
            logger.println(message);
        }
    }
}
