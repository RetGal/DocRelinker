package org.freedom.log;

import org.freedom.log.console.ConsoleAppender;

/**
 * Minimal logging interface
 */
public class Log
{
    private static final boolean DEBUG = false;

    private static LogAppender out;

    static
    {
        out = new ConsoleAppender();
    }

    public static boolean isDebugEnabled()
    {
        return DEBUG;
    }

    public static void debug(String message)
    {
        if (DEBUG)
        {
            out.append(LogLevel.DEBUG, message);
        }
    }

    public static void debug(String message, Throwable error)
    {
        if (DEBUG)
        {
            out.append(LogLevel.DEBUG, message, error);
        }
    }

    public static void info(String message)
    {
        out.append(LogLevel.INFO, message);
    }

    public static void warn(String message)
    {
        out.append(LogLevel.WARN, message);
    }

    public static void warn(Throwable error)
    {
        out.append(LogLevel.WARN, error.getMessage(), error);
    }

    public static void warn(String message, Throwable error)
    {
        out.append(LogLevel.WARN, message, error);
    }

    public static void error(String message)
    {
        out.append(LogLevel.ERROR, message);
    }

    public static void error(String message, Throwable error)
    {
        out.append(LogLevel.ERROR, message, error);
    }

    public static void fatal(String message)
    {
        out.append(LogLevel.FATAL, message);
    }

    public static void fatal(String message, Throwable error)
    {
        out.append(LogLevel.FATAL, message, error);
    }

}
