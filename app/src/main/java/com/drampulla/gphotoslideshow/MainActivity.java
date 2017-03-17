package com.drampulla.gphotoslideshow;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.util.Timer;

/**
 * The main entry point for the application to start the slideshow.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Default logger.
     */
    private static final Logger LOGGER = new Logger(MainActivity.class);

    /**
     * Class that manages all Google Photos.
     */
    private final PicasaManager picasaManager = new PicasaManager();

    /**
     * Iterator to walk forward and backward through my Google Photos.
     */
    private SlideshowIterator slideshowIterator;

    /**
     * Change slides every X seconds (based on preferences).
     */
    private ChangeSlideScheduledJob changeSlideScheduledJob;

    /**
     * Update the time on the screen every second.
     */
    private UpdateTimeTimerTask updateTimeTimerTask;

    /**
     * Handler for doing deferred operations (updating screen text, etc.).
     */
    private Handler handler;

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the "settings" toolbar
        initializeSettingsToolbar();

        initializeImageSwitcher();

        // add handler for detecting swipe left/right
        initializeGestureDetection();

        handler = new Handler();

        picasaManager.initialize(MainActivity.this, new PicasaManager.InitializedCallback() {
            @Override
            public void callback() {
                try {
                    slideshowIterator = picasaManager.createSlideshowIterator();
                    changeSlideScheduledJob = new ChangeSlideScheduledJob(slideshowIterator, MainActivity.this, handler);
                    updateTimeTimerTask = new UpdateTimeTimerTask(MainActivity.this, handler);
                } catch (IOException | ServiceException e) {
                    LOGGER.e("Failed to create slideshow iterator", e);
                }
            }
        });
    }

    /**
     *
     */
    private void initializeImageSwitcher() {
        final ImageSwitcher imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return imageView;
            }
        });

        // TODO: Figure out in/out animations
        //imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));
        //imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right));
    }

    /**
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handle the item selected to bring up the "Settings" screen.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Setup the toolbar to get to the Settings screen.
     */
    private void initializeSettingsToolbar() {
        final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

    }

    /**
     * Setup Gesture Detection so that we can
     *      swipe right: slideshow.next
     *      swipe_left: slideshow.prev
     *      touch : bring up settings toolbar
     */
    private void initializeGestureDetection() {
        ImageSwitcher imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher);
        final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // only display the toolbar when the user clicks on the screen
                final Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
                if (myToolbar.getVisibility() == Toolbar.VISIBLE) {
                    myToolbar.setVisibility(Toolbar.GONE);
                } else {
                    myToolbar.setVisibility(Toolbar.VISIBLE);
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }

                return false;
            }
        });

        imageSwitcher.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetectorCompat.onTouchEvent(motionEvent);
                return true;
            }
        });

    }


    private void onSwipeRight() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity.this.changeSlideScheduledJob.previousSlide();
                return null;
            }
        }.execute();
    }

    private void onSwipeLeft() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                MainActivity.this.changeSlideScheduledJob.nextSlide();
                return null;
            }
        }.execute();
    }

}