package com.drampulla.gphotoslideshow;

import android.util.Log;

/**
 * Creating a simplified logger that actually logs the class name with the message
 * like most normal loggers do.
 */

public class Logger {
    private String tag;

    public Logger(Class<?> cls) {
        tag = cls.getCanonicalName();
    }

    public void d(String msg) { Log.d(tag, msg); }
    public void d(String msg, Throwable t) { Log.d(tag, msg, t); }

    public void e(String msg) { Log.e(tag, msg); }
    public void e(String msg, Throwable t) { Log.e(tag, msg, t); }

    public void i(String msg) { Log.i(tag, msg); }
    public void i(String msg, Throwable t) { Log.i(tag, msg, t); }

    public void v(String msg) { Log.v(tag, msg); }
    public void v(String msg, Throwable t) { Log.v(tag, msg, t); }

    public void w(String msg) { Log.w(tag, msg); }
    public void w(String msg, Throwable t) { Log.w(tag, msg, t); }

    public void wtf(String msg) { Log.wtf(tag, msg); }
    public void wtf(String msg, Throwable t) { Log.wtf(tag, msg, t); }
}
