package com.drampulla.gphotoslideshow;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.ImageSwitcher;
import android.widget.TextView;

import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Move to the next Google Photo every X seconds.
 */
public class ChangeSlideScheduledJob {

    /**
     * Default logger.
     */
    private static final Logger LOGGER = new Logger(ChangeSlideScheduledJob.class);
    /**
     * Date format of the picture's creation date.
     */
    private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("MM/dd/yyyy");
    /**
     * Scheduling executor.
     */
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(1);

    private SlideshowIterator slideshowIterator;
    private Activity mainActivity;
    private Handler replaceImageHandler;
    private ImageSwitcher imageSwitcher;
    private ScheduledFuture<?> future;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    /**
     *
     * @param slideshowIterator
     *          The slideshow iterator that can get the next PhotoEntry
     * @param mainActivity
     *          The mainActivity that has the imageSwitcher we are putting the photo into
     * @param replaceImageHandler
     *          The handler used to manipulate the mainActivity
     */
    public ChangeSlideScheduledJob(SlideshowIterator slideshowIterator, Activity mainActivity, Handler replaceImageHandler) {
        this.slideshowIterator = slideshowIterator;
        this.mainActivity = mainActivity;
        this.replaceImageHandler = replaceImageHandler;

        changeScheduledTime();

        // Keeping this as a member variable because the SharedPreferences only keep this listener
        // as a weak reference.
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (PreferenceConstants.DISPLAY_INTERVAL_KEY.equals(s)) {
                    LOGGER.d("Preference changed, so update the timer");
                    changeScheduledTime();
                }
            }
        };

        // handle if the preference changes
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    /**
     * The preference changed, so cancel the timer and create a new scheduled job with the new
     * preference time.
     */
    private void changeScheduledTime() {

        if (future != null) {
            future.cancel(false);
        }

        String displayInterval = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext()).getString(PreferenceConstants.DISPLAY_INTERVAL_KEY, "30");
        future = EXECUTOR.scheduleAtFixedRate(new Runnable() {
                                                          @Override
                                                          public void run() {
                                                              try {
                                                                  nextSlide();
                                                              } catch (Throwable t) {
                                                                  LOGGER.e("Something bad happened while getting next slide", t);
                                                                  AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                                                                  builder.setTitle("NextSlide error")
                                                                          .setMessage(t.getMessage())
                                                                          .setCancelable(true)
                                                                          .create().show();
                                                              }
                                                          }
                                                      },
                                            Long.parseLong(displayInterval),
                                            Long.parseLong(displayInterval),
                                            TimeUnit.SECONDS);
    }

    /**
     * Advance the display to the next photo from Google.
     */
    public void nextSlide() {
        PhotoEntry photoEntry = slideshowIterator.next();
        displaySlide(photoEntry);
    }

    /**
     * Go backward on the display to the previous photo from Google.
     */
    public void previousSlide() {
        PhotoEntry photoEntry = slideshowIterator.previous();
        displaySlide(photoEntry);
    }

    /**
     * Put a photo entry onto the ImageSwitcher.
     *
     * @param photoEntry
     *      The photo to display
     */
    private void displaySlide(final PhotoEntry photoEntry) {
        String url = photoEntry.getMediaContents().get(0).getUrl();
        try {
            final Bitmap bmp = BitmapFactory.decodeStream(new URL(url).openStream());

            replaceImageHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final BitmapDrawable drawable = new BitmapDrawable(mainActivity.getResources(), bmp);
                        imageSwitcher = (ImageSwitcher) mainActivity.findViewById(R.id.imageSwitcher);
                        imageSwitcher.setImageDrawable(drawable);

                        final TextView textViewPhotoInfo = (TextView) mainActivity.findViewById(R.id.image_display_details_text);
                        if (PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext()).getBoolean(PreferenceConstants.SHOW_PHOTO_DESCRIPTION, false)) {
                            textViewPhotoInfo.setText(getPhotoDescription(photoEntry));
                        } else {
                            textViewPhotoInfo.setText("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        } catch (IOException e) {
            LOGGER.e("Unable to retrieve image " + url, e);
        }
    }

    /**
     * Turn the photo's details into a description to show on top of the image.
     * @param photoEntry
     *          The photo being displayed
     * @return
     *          The date/time the photo was taken and the title of the file.
     */
    private String getPhotoDescription(PhotoEntry photoEntry) {

        String dateTime = null;
        try {
            dateTime = DATE_TIME_FORMATTER.format(photoEntry.getTimestamp());
        } catch (ServiceException e) {
            LOGGER.w("Problem getting photo timestamp", e);
        }

        return dateTime + "\n"
                + photoEntry.getTitle().getPlainText();
    }
}
