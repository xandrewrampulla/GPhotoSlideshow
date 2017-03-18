package com.drampulla.gphotoslideshow;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles all interactions with Google Photos via the "Picasa Web API".  It is
 * required to be initialized in the main activities onCreate method so that it has the chance
 * to prompt the user for permissions necessary.
 */
public class PicasaManager {

    /**
     * Default logger.
     */
    private static final Logger LOGGER = new Logger(PicasaManager.class);

    /**
     * Picasa web API constants
     */
    private static final String URL_PREFIX = "https://picasaweb.google.com/data/feed/api/user/";
    private static final String PICASA_OAUTH_TYPE = "oauth2:https://picasaweb.google.com/data/";

    /**
     * The google provided PicasawebService that does most of the work for me.
     */
    private PicasawebService picasawebService;
    /**
     * The google username required to access the photos.
     */
    private String username;

    /**
     * Preferences for storing various information.
     */
    private SharedPreferences sharedPreferences;

    /**
     * Initialize the service by finding the right username and getting appropriate
     * OAuth keys from Google.
     *
     * @param mainActivity
     *          The activity that can be used to prompt the user for permissions.
     *
     * @param initializedCallback
     *          The method that should be called when the AccountManager authorizes
     *          this application (this happens outside of the main UI thread).
     */
    public void initialize(final Activity mainActivity, final InitializedCallback initializedCallback) {

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext());

        try {
            ActivityCompat.requestPermissions(mainActivity,
                    new String[]{android.Manifest.permission.GET_ACCOUNTS},
                    0);

            AccountManager am = (AccountManager) mainActivity.getSystemService(Activity.ACCOUNT_SERVICE);
            Account[] accounts = am.getAccounts();

            LOGGER.v("Account length:" + accounts.length);
            for (Account a : accounts) {
                LOGGER.v("Account : " + a.name + ", " + a.type + ", " + a.describeContents());
            }

            if (accounts.length < 1) {
                throw new RuntimeException("PicasaManager is unable to find any accounts on this system");
            }

            this.username = accounts[0].name;


            AccountManagerCallback<Bundle> cb = new AccountManagerCallback<Bundle>() {
                @Override
                public void run(final AccountManagerFuture<Bundle> accountManagerFuture) {
                    try {
                        if (accountManagerFuture.getResult() == null) {
                            throw new RuntimeException("Failed to get the account OAuth token, but not sure why");
                        }
                        String authToken = accountManagerFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);
                        String accountName = accountManagerFuture.getResult().getString(AccountManager.KEY_ACCOUNT_NAME);
                        String accountType = accountManagerFuture.getResult().getString(AccountManager.KEY_ACCOUNT_TYPE);
                        LOGGER.v("Got OAuth token for " + accountName + " -- " + accountType + " -- Token=" + authToken);

                        picasawebService = new PicasawebService("drampulla.example.com.helloworld");
                        picasawebService.setAuthSubToken(accountManagerFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN));

                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                initializedCallback.callback();
                                return null;
                            }
                        }.execute();
                    } catch (Throwable t) {
                        LOGGER.wtf("Unable to construct picasa web service because " + t.getMessage(), t);
                    }
                }
            };
            am.getAuthToken(accounts[0], PICASA_OAUTH_TYPE, null, mainActivity, cb, null);


        } catch (SecurityException e) {
            LOGGER.wtf("Security problem creating the PicasaManager", e);
        } catch (Exception e) {
            LOGGER.wtf("General problem creating the PicasaManager", e);
        }
    }


    /**
     *
     * @return all of the albums available via picasa API
     */
    public List<AlbumEntry> getAlbums() {
        List<AlbumEntry> rc = new ArrayList<>();

        long start = System.currentTimeMillis();
        try {
            UserFeed uf = picasawebService.getFeed(new URL(URL_PREFIX + username), UserFeed.class);
            for (GphotoEntry ge : uf.getEntries()) {
                AlbumEntry ae = new AlbumEntry(ge);
                rc.add(ae);

            }

            LOGGER.v(String.format("Got all of the albums from Picasa in %dms.", System.currentTimeMillis()-start));
        } catch (IOException |ServiceException e) {
            LOGGER.wtf("Failed to get the album details because " + e.getMessage(), e);
            throw new RuntimeException("Failed to get the album details", e);
        }

        return rc;

    }

    /**
     *
     * @return Just the names of the albums available.  This is useful for displaying in a selection list.
     */
    public List<String> getAlbumNames() {
        List<String> rc = new ArrayList<>();
        for (AlbumEntry ae : getAlbums()) {
            rc.add(ae.getName());
        }
        return rc;
    }

    /**
     *
     * @param ae
     *      The album to retrieve the photo details from
     * @return
     *      List of photo details of the photos in the specified album.
     */
    public List<PhotoEntry> getPhotoEntries(AlbumEntry ae) {
        List<PhotoEntry> rc = new ArrayList<>();
        long start = System.currentTimeMillis();
        try {
            AlbumFeed feed = ae.getFeed();
            for (GphotoEntry ge : feed.getEntries()) {
                rc.add(new PhotoEntry(ge));
            }
            LOGGER.v(String.format("Got all of the photo entries from Picasa in %dms.", System.currentTimeMillis()-start));
        } catch (IOException | ServiceException e) {
            LOGGER.wtf("Failed to get photos because " + e.getMessage(), e);
            throw new RuntimeException("Failed to get the photos", e);
        }

        return rc;
    }

    /**
     *
     * @return a SlideshowIterator to walk over the albums and photos within those albums
     *
     * @throws MalformedURLException
     * @throws IOException
     * @throws ServiceException
     */
    public SlideshowIterator createSlideshowIterator() throws MalformedURLException, IOException, ServiceException {
        return new SlideshowIterator(sharedPreferences, picasawebService.getFeed(new URL(URL_PREFIX + username), UserFeed.class));
    }

    /**
     * Class to help with callback after the account manager gets initialized
     */
    public static interface InitializedCallback {
        void callback();
    }
}
