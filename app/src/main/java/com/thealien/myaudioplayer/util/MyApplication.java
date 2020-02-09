package com.thealien.myaudioplayer.util;

import android.app.Application;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/*
this class will contain the main list of songs to be played so when the user
clicks on an item to play all the list will be saved on this list of mediaBrowserCompat.MediaItem objects
and it will be cleared and reset once the user selects another song from another playlist
* */
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    private static MyApplication mInstance;
    private static List<MediaBrowserCompat.MediaItem> mMediaItems = new ArrayList<>();
    private static TreeMap<String, MediaMetadataCompat> mTreeMap = new TreeMap<>();

    public static MyApplication getInstance(){
        if(mInstance == null){
            mInstance = new MyApplication();
        }
        return mInstance;
    }

    public List<MediaBrowserCompat.MediaItem> getMediaItems(){
        return mMediaItems;
    }

    public TreeMap<String, MediaMetadataCompat> getTreeMap(){
        return mTreeMap;
    }

    public void setMediaItems(List<MediaMetadataCompat> mediaItems){
        mMediaItems.clear();
        for(MediaMetadataCompat item: mediaItems){
            Log.d(TAG, "setMediaItems: called: adding media item: " + item.getDescription());
            mMediaItems.add(
                    new MediaBrowserCompat.MediaItem(
                            item.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            );
            mTreeMap.put(item.getDescription().getMediaId(), item);
        }
    }

    public MediaMetadataCompat getMediaItem(String mediaId){
        return mTreeMap.get(mediaId);
    }
}
