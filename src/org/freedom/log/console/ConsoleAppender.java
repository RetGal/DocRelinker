package org.freedom.log.console;

import org.freedom.log.LogAppender;
import org.freedom.log.LogLevel;

public class ConsoleAppender extends LogAppender
{

    public synchronized void append(LogLevel level, String message, Throwable error)
    {
        System.out.println(format(level, message));

        if (error != null)
        {
            error.printStackTrace(System.out);
        }
    }
}
