package com.drampulla.gphotoslideshow;

import android.app.Activity;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is just meant to update the time on the screen.
 */
public class UpdateTimeTimerTask {

    private static final Logger LOGGER = new Logger(UpdateTimeTimerTask.class);

    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    private final Activity mainActivity;
    private final Handler handler;

    public UpdateTimeTimerTask(Activity mainActivity, Handler handler) {
        this.mainActivity = mainActivity;
        this.handler = handler;
        EXECUTOR.scheduleAtFixedRate(new Runnable() {
                                                  @Override
                                                  public void run() { updateTime(); }
                                              },
                1,
                1,
                TimeUnit.SECONDS);
    }

    private void updateTime() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) mainActivity.findViewById(R.id.image_display_time_text);
                if (PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext()).getBoolean(PreferenceConstants.SHOW_TIME, false)) {
                    String dateFormat = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext()).getString(PreferenceConstants.TIME_FORMAT, "HH:mm:ss");
                    try {
                        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(dateFormat);
                        String dateText = dateTimeFormatter.format(new Date());
                        textView.setText(dateText);
                    } catch (Exception e) {
                        LOGGER.w("Failed to parse date " + dateFormat, e);
                        dateFormat = "HH:mm:ss";
                        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(dateFormat);
                        textView.setText(dateTimeFormatter.format(new Date()));
                    }
                } else {
                    textView.setText("");
                }
            }
        });
    }
}
