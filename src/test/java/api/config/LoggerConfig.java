package api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;

public class LoggerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LoggerConfig.class);
    private static boolean initialized = false;

    public static void initialize(boolean debugMode) {
        if (initialized) return;

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();

        // Create console appender
        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setContext(context);
        consoleAppender.setName("CONSOLE");

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{HH:mm:ss.SSS} [%-15.15thread] %-5level %-40.40logger{40} - %msg%n");
        encoder.start();

        consoleAppender.setEncoder(encoder);
        consoleAppender.start();

        // Get root logger and configure
        ch.qos.logback.classic.Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(consoleAppender);

        if (debugMode) {
            rootLogger.setLevel(Level.DEBUG);
            // Reduce noise from Apache HTTP Client
            context.getLogger("org.apache.hc").setLevel(Level.INFO);
            context.getLogger("org.apache.http").setLevel(Level.INFO);
        } else {
            rootLogger.setLevel(Level.INFO);
            // Suppress Apache HTTP Client logs completely
            context.getLogger("org.apache.hc").setLevel(Level.WARN);
            context.getLogger("org.apache.http").setLevel(Level.WARN);
        }

        initialized = true;
        logger.info("Logging initialized in {} mode", debugMode ? "DEBUG" : "INFO");
    }

    public static void setDebugMode(boolean debugMode) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        if (debugMode) {
            context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);
            context.getLogger("org.apache.hc").setLevel(Level.INFO);
        } else {
            context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
            context.getLogger("org.apache.hc").setLevel(Level.WARN);
        }

        logger.info("Logging level set to {}", debugMode ? "DEBUG" : "INFO");
    }
}