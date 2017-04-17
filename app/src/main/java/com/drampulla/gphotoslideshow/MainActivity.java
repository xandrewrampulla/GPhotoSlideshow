package com.drampulla.gphotoslideshow;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.gdata.util.ServiceException;

import java.io.IOException;

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
    private final static PicasaManager picasaManager = new PicasaManager();

    /**
     * Iterator to walk forward and backward through my Google Photos.
     */
    private static SlideshowIterator slideshowIterator;

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
     * Handler for all gestures on the MainActivity
     */
    private GestureDetectorCompat gestureDetectorCompat;

    /**
     * Preference listener for handling changes in Animation type.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener animationTypePreferenceChangeListener;


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
        gestureDetectorCompat = new GestureDetectorCompat(this, new MainGestureListener());

        handler = new Handler();

        // initialize the picasa web service
        initializePicasa();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
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

        initializeAnimations(imageSwitcher);
    }

    /**
     *
     * @param imageSwitcher
     */
    private void initializeAnimations(final ImageSwitcher imageSwitcher) {
        String animationType = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceConstants.ANIMATION_TYPE, "fadein,fadeout");
        String[] split = animationType.split(",");
        // not bothering with checking split, since this is in my control
        int inR = getResources().getIdentifier(split[0], "anim", this.getPackageName());
        int outR = getResources().getIdentifier(split[1], "anim", this.getPackageName());

        imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, inR));
        imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, outR));

        // Keeping this as a member variable because the SharedPreferences only keep this listener
        // as a weak reference.
        animationTypePreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                if (PreferenceConstants.ANIMATION_TYPE.equals(s)) {
                    LOGGER.d("Preference changed, so update the animation style");
                    initializeAnimations(imageSwitcher);
                }
            }
        };
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(animationTypePreferenceChangeListener);
    }


    private void initializePicasa() {
        picasaManager.initialize(MainActivity.this, new PicasaManager.InitializedCallback() {
            @Override
            public void callback() {
                try {
                    if (slideshowIterator == null) {
                        slideshowIterator = picasaManager.createSlideshowIterator();
                    }
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

        LOGGER.d("Was an option actually selected?");

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            LOGGER.d("Should be laucnhing settings, does this work?");
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
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return super.onTouchEvent(event);
    }








    public class MainGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // only display the toolbar when the user clicks on the screen
            Toolbar toolbar = (Toolbar) MainActivity.this.findViewById(R.id.my_toolbar);
            if (toolbar.getVisibility() == Toolbar.VISIBLE) {
                toolbar.setVisibility(Toolbar.GONE);
            } else {
                toolbar.setVisibility(Toolbar.VISIBLE);
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

}
