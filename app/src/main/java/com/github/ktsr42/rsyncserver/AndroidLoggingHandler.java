package com.github.ktsr42.rsyncserver;

import android.util.Log;
import java.util.logging.*;

/**
 * Make JUL work on Android.
 */
public class AndroidLoggingHandler extends Handler {

  public static void reset(Handler rootHandler) {
    Logger rootLogger = LogManager.getLogManager().getLogger("");
    Handler[] handlers = rootLogger.getHandlers();
    for (Handler handler : handlers) {
      rootLogger.removeHandler(handler);
    }
    rootLogger.addHandler(rootHandler);
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() {
  }

  @Override
  public void publish(LogRecord record) {
    if (!super.isLoggable(record))
      return;

    String name = record.getLoggerName();
    int maxLength = 30;
    String tag = name.length() > maxLength ? name.substring(name.length() - maxLength) : name;

    try {
      int level = getAndroidLevel(record.getLevel());
      Log.println(level, tag, record.getMessage());
      if (record.getThrown() != null) {
        Log.println(level, tag, Log.getStackTraceString(record.getThrown()));
      }
    } catch (RuntimeException e) {
      Log.e("AndroidLoggingHandler", "Error logging message.", e);
    }
  }

  static int getAndroidLevel(Level level) {
    int value = level.intValue();

    if (value >= Level.SEVERE.intValue()) {
      return Log.ERROR;
    } else if (value >= Level.WARNING.intValue()) {
      return Log.WARN;
    } else if (value >= Level.INFO.intValue()) {
      return Log.INFO;
    } else {
      return Log.DEBUG;
    }
  }
}
