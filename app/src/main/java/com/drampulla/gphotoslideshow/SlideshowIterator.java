package com.drampulla.gphotoslideshow;

import android.content.SharedPreferences;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.regex.Pattern;

/**
 * Iterator to walk through the photos in various Google Photo albums.  As we reach
 * the end of one photo album we automatically move to the next album.
 */
public class SlideshowIterator implements ListIterator<PhotoEntry> {

    /**
     * Default logger.
     */
    private static final Logger LOGGER = new Logger(SlideshowIterator.class);

    /**
     * Feed of all albums the in the user's account
     */
    private UserFeed albumFeed;
    /**
     * Iterator that keeps track of the current album we are working on.
     */
    private ListIterator<GphotoEntry> albumIterator;
    /**
     * The current album we are working on.
     */
    private AlbumEntry currentAlbum;
    /**
     * The iterator that keeps track of the current photo within the album
     * that we are working on.
     */
    private ListIterator<GphotoEntry> photoIterator;

    /**
     * Counter to keep track of how many pictures into the slideshow we are.
     * The intent is to store this value somewhere and restart a slideshow
     * from where it left off.
     */
    private int listIndex;

    /**
     * preferences for storing "listIndex" so we can remember where we left off in the iterator.
     */
    private SharedPreferences sharedPreferences;


    /**
     * Default constructor.
     *
     * @param albumFeed
     *          The album feed to iterate on.
     * @throws IOException
     * @throws ServiceException
     */
    public SlideshowIterator(SharedPreferences sharedPreferences, UserFeed albumFeed) throws IOException, ServiceException {
        this.sharedPreferences = sharedPreferences;
        this.albumFeed = albumFeed;
        this.albumIterator = this.albumFeed.getEntries().listIterator();
        if (!this.albumIterator.hasNext()) {
            throw new RuntimeException("There are no albums available.");
        }
        this.listIndex = 0;
        this.currentAlbum = new AlbumEntry(this.albumIterator.next());
        this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();

        LOGGER.d("Starting with album " + this.currentAlbum.getTitle().getPlainText());

        advanceToListIndex(sharedPreferences.getInt(PreferenceConstants.SLIDESHOW_INDEX, 0));
    }

    /**
     * Advance forward to the specified listIndex.
     *
     * @param targetIndex
     *          The index of the photo to display.
     */
    private void advanceToListIndex(int targetIndex) {
        while (listIndex < targetIndex) {
            next();
        }
    }

    @Override
    public boolean hasNext() {

        boolean rc = this.photoIterator.hasNext();
        if (!rc) {
            rc = this.albumIterator.hasNext();
        }
        return rc;
    }

    @Override
    public PhotoEntry next() {
        PhotoEntry rc = null;
        try {
            if (this.photoIterator.hasNext()) {
                rc = new PhotoEntry(this.photoIterator.next());
            } else {
                this.currentAlbum = nextAlbumEntry();
                LOGGER.d("Using album " + this.currentAlbum.getTitle().getPlainText());
                this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();
                rc =  next();
            }

            listIndex++;
            sharedPreferences.edit().putInt(PreferenceConstants.SLIDESHOW_INDEX, listIndex).commit();
            LOGGER.d("Next index=" + listIndex);
        } catch (IOException | ServiceException e) {
            throw new RuntimeException("Unable to get next photo", e);
        }

        return rc;
    }

    /**
     * Skip over any album entries that don't match the include pattern or do match the exclude pattern
     * @return
     *          The next album entry to use.
     */
    private AlbumEntry nextAlbumEntry() {

        // I want to make sure empty string is the same as include everything
        String includeRegex = sharedPreferences.getString(PreferenceConstants.INCLUDE_REGEX, ".*");
        includeRegex = includeRegex.equals("") ? ".*" : includeRegex;

        Pattern excludePattern = Pattern.compile(sharedPreferences.getString(PreferenceConstants.EXCLUDE_REGEX, ""));
        Pattern includePattern = Pattern.compile(includeRegex);

        if (!this.albumIterator.hasNext()) {
            // we ran over all albums, so rotate back to start
            this.albumIterator = this.albumFeed.getEntries().listIterator();
            listIndex = 0;
        }


        AlbumEntry ae = new AlbumEntry(this.albumIterator.next());
        while (!includePattern.matcher(ae.getTitle().getPlainText()).matches()
                || excludePattern.matcher(ae.getTitle().getPlainText()).matches()) {
            LOGGER.d("Skipping over album " + ae.getTitle().getPlainText());
            ae = new AlbumEntry(this.albumIterator.next());
        }

        return ae;
    }

    @Override
    public boolean hasPrevious() {
        boolean rc = this.photoIterator.hasPrevious();
        if (!rc) {
            rc = this.albumIterator.hasPrevious();
        }
        return rc;
    }

    @Override
    public PhotoEntry previous() {
        PhotoEntry rc = null;
        try {
            if (this.photoIterator.hasPrevious()) {
                rc = new PhotoEntry(this.photoIterator.previous());
            } else {
                this.currentAlbum = new AlbumEntry(this.albumIterator.previous());
                this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();
                rc = new PhotoEntry(this.photoIterator.previous());
            }

            listIndex--;
            sharedPreferences.edit().putInt(PreferenceConstants.SLIDESHOW_INDEX, listIndex).commit();
        } catch (IOException | ServiceException e) {
            throw new RuntimeException("Unable to get next photo", e);
        }
        return rc;
    }

    @Override
    public int nextIndex() {
        return this.listIndex + 1;
    }

    @Override
    public int previousIndex() {
        return listIndex - 1;
    }

    @Override
    public void set(PhotoEntry photoEntry) {
        throw new UnsupportedOperationException("set is not supported.");
    }

    @Override
    public void add(PhotoEntry photoEntry) {
        throw new UnsupportedOperationException("add is not supported.");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove it not supported.");
    }
}

