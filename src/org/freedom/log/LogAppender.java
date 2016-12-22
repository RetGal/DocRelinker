package org.freedom.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class LogAppender
{
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");


    public String format(LogLevel level, String message)
    {
        message = (message == null) ? "" : message;
        return String.format("[%16.16s] [%5.5s] (%s) %s", Thread.currentThread().getName(), level, DATE_FORMAT.format(new Date()), message);
    }

    public void append(LogLevel level, String message)
    {
        this.append(level, message, null);
    }

    public abstract void append(LogLevel level, String message, Throwable error);


}
