package com.thealien.myaudioplayer.client;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import com.thealien.myaudioplayer.player.PlayerController;

import java.util.List;

public class MediaBrowserHelper {


    private static final String TAG = "MediaBrowserHelper";
    private Context context;
    private final Class<? extends MediaBrowserServiceCompat> mediaBrowserServiceClass;
    private boolean wasConfigurationChanged;
    private boolean isFirstLaunch;

    private MediaBrowserCompat mediaBrowserCompat;
    private MediaControllerCompat mediaControllerCompat;

    private MediaBrowserSubscriptionCallback mediaBrowserSubscriptionCallback;
    private MediaBrowserConnectionCallback mediaBrowserConnectionCallback;
    private MediaControllerCallback mediaControllerCallback;
    private MediaBrowserHelperCallback mediaBrowserHelperCallback;

    private PlayerController playerController;

    public MediaBrowserHelper(Context context, Class<? extends MediaBrowserServiceCompat> mediaBrowserServiceClass) {
        this.context = context;
        this.mediaBrowserServiceClass = mediaBrowserServiceClass;
        mediaBrowserConnectionCallback = new MediaBrowserConnectionCallback();
        mediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
        mediaBrowserSubscriptionCallback = new MediaBrowserSubscriptionCallback();
        mediaControllerCallback = new MediaControllerCallback();
    }

    public void setMediaBrowserHelperCallback(MediaBrowserHelperCallback callback){
        this.mediaBrowserHelperCallback = callback;
    }

    public void subscribeToNewPlaylist(String playlistId){
        mediaBrowserCompat.subscribe(playlistId,mediaBrowserSubscriptionCallback);
    }

    public void setPlayerController(PlayerController playerController){
        this.playerController = playerController;
    }

    public void setSpeed(float speed){
        playerController.speedUp(speed);
    }

    public void onStart(boolean wasConfigurationChanged,boolean isFirstLaunch) {
        this.wasConfigurationChanged = wasConfigurationChanged;
        this.isFirstLaunch = isFirstLaunch;
        if (mediaBrowserCompat == null) {
            mediaBrowserCompat = new MediaBrowserCompat(
                    context,
                    new ComponentName(context, mediaBrowserServiceClass),
                    mediaBrowserConnectionCallback,
                    null
            );
            mediaBrowserCompat.connect();
        }
        Log.d(TAG, "onStart: CALLED : creating media broswer and connecting");
    }

    public void onStop() {
        if (mediaBrowserCompat.isConnected() && mediaBrowserCompat != null) {
            mediaBrowserCompat.disconnect();
            mediaBrowserCompat = null;
        }
        if (mediaControllerCompat != null) {
            mediaControllerCompat.unregisterCallback(mediaControllerCallback);
            mediaControllerCompat = null;
        }
        Log.d(TAG, "onStop: CALLED: Releasing MediaController, Disconnecting from MediaBrowser");
    }

    private class MediaBrowserConnectionCallback extends MediaBrowserCompat.ConnectionCallback {
        @Override
        public void onConnected() {
            super.onConnected();
            Log.d(TAG, "onConnected: CALLED");
            try {
                mediaControllerCompat = new MediaControllerCompat(context, mediaBrowserCompat.getSessionToken());
                mediaControllerCompat.registerCallback(mediaControllerCallback);

                mediaBrowserHelperCallback.onMediaControllerConnected(mediaControllerCompat);

            } catch (RemoteException e) {
                e.printStackTrace();
                Log.d(TAG, String.format("onConnected: Problem: %s", e.toString()));
                throw new RuntimeException(e);
            }

            mediaBrowserCompat.subscribe(mediaBrowserCompat.getRoot(), mediaBrowserSubscriptionCallback);
            //mediaControllerCompat.registerCallback(mediaControllerCallback);
            Log.d(TAG, "onConnected: subscribing to : " + mediaBrowserCompat.getRoot());
        }
    }

    private class MediaBrowserSubscriptionCallback extends MediaBrowserCompat.SubscriptionCallback {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaBrowserCompat.MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: CALLED" + parentId + ", " + children.toString());

            if(!wasConfigurationChanged && isFirstLaunch){
                Log.d(TAG, "onChildrenLoaded: is first launch ?" + isFirstLaunch);
                for (MediaBrowserCompat.MediaItem mediaItem : children){
                    Log.d(TAG, "onChildrenLoaded: CALLED:adding queue item: " + mediaItem.getMediaId());
                    mediaControllerCompat.addQueueItem(mediaItem.getDescription());
                }
            }
        }
    }

    private class MediaControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            Log.d(TAG, "onPlaybackStateChanged: CAlLED");
            if(mediaBrowserHelperCallback != null){
                mediaBrowserHelperCallback.onPlaybackStateChanged(state);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            Log.d(TAG, "onMetadataChanged: CALLED");
            if(mediaBrowserHelperCallback != null){
                mediaBrowserHelperCallback.onMetadataChanged(metadata);
            }
        }
    }

    public MediaControllerCompat.TransportControls getTransportControls(){
        if(mediaControllerCompat == null){
            Log.d(TAG, "getTransportControls: mediaController is null!");
            throw new IllegalStateException("mediaController is null!");
        }
        return mediaControllerCompat.getTransportControls();
    }
}
