package com.drampulla.gphotoslideshow;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Iterator to walk through the photos in various Google Photo albums.  As we reach
 * the end of one photo album we automatically move to the next album.
 */
public class SlideshowIterator implements ListIterator<PhotoEntry> {

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
     * TODO: The intent is to store this value somewhere and restart a slideshow
     * from where it left off.
     */
    private int listIndex;

    /**
     * Default constructor.
     *
     * @param albumFeed
     *          The album feed to iterate on.
     * @throws IOException
     * @throws ServiceException
     */
    public SlideshowIterator(UserFeed albumFeed) throws IOException, ServiceException {
        this.albumFeed = albumFeed;
        this.albumIterator = this.albumFeed.getEntries().listIterator();
        if (!this.albumIterator.hasNext()) {
            throw new RuntimeException("There are no albums available.");
        }
        this.listIndex = 0;
        this.currentAlbum = new AlbumEntry(this.albumIterator.next());
        this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();
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
        try {
            if (this.photoIterator.hasNext()) {
                return new PhotoEntry(this.photoIterator.next());
            } else {
                this.currentAlbum = new AlbumEntry(this.albumIterator.next());
                this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();
                return new PhotoEntry(this.photoIterator.next());
            }
        } catch (IOException | ServiceException e) {
            throw new RuntimeException("Unable to get next photo", e);
        }
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
        try {
            if (this.photoIterator.hasPrevious()) {
                return new PhotoEntry(this.photoIterator.previous());
            } else {
                this.currentAlbum = new AlbumEntry(this.albumIterator.previous());
                this.photoIterator = this.currentAlbum.getFeed().getEntries().listIterator();
                return new PhotoEntry(this.photoIterator.previous());
            }
        } catch (IOException | ServiceException e) {
            throw new RuntimeException("Unable to get next photo", e);
        }
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

