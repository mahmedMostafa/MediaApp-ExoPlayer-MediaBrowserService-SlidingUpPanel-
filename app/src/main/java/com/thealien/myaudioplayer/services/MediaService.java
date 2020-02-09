package com.thealien.myaudioplayer.services;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.thealien.myaudioplayer.R;
import com.thealien.myaudioplayer.client.MediaBrowserHelper;
import com.thealien.myaudioplayer.notifications.MediaNotificationManager;
import com.thealien.myaudioplayer.player.MediaPlayerAdapter;
import com.thealien.myaudioplayer.player.PlaybackInfoListener;
import com.thealien.myaudioplayer.player.PlayerAdapter;
import com.thealien.myaudioplayer.player.PlayerController;
import com.thealien.myaudioplayer.util.MediaLibrary;
import com.thealien.myaudioplayer.util.MyApplication;
import com.thealien.myaudioplayer.util.MyPreferenceManager;

import java.util.ArrayList;
import java.util.List;

import static com.thealien.myaudioplayer.util.Constants.CUSTOM_ACTION_SPEED;
import static com.thealien.myaudioplayer.util.Constants.DECREASE_15;
import static com.thealien.myaudioplayer.util.Constants.INCREASE_15;
import static com.thealien.myaudioplayer.util.Constants.MEDIA_QUEUE_POSITION;
import static com.thealien.myaudioplayer.util.Constants.QUEUE_NEW_PLAYLIST;
import static com.thealien.myaudioplayer.util.Constants.SEEK_BAR_MAX;
import static com.thealien.myaudioplayer.util.Constants.SEEK_BAR_PROGRESS;
import static com.thealien.myaudioplayer.util.Constants.SPEED_VALUE;

public class MediaService extends MediaBrowserServiceCompat implements PlayerController {


    private static final String TAG = "MediaService";
    private MediaSessionCompat mediaSessionCompat;
    private PlayerAdapter playback;
    private MediaNotificationManager mediaNotificationManager;
    private boolean isServiceRunning;


    //private MediaLibrary mediaLibrary;
    private MyApplication myApplication;
    private MyPreferenceManager myPreferenceManager;

    private MediaBrowserHelper mediaBrowserHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        myPreferenceManager = MyPreferenceManager.getInstance(this);
        //mediaLibrary = new MediaLibrary();
        myApplication = MyApplication.getInstance();
        mediaSessionCompat = new MediaSessionCompat(this, TAG);
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSessionCompat.setCallback(new MediaSessionCallback());

        setSessionToken(mediaSessionCompat.getSessionToken());
        playback = new MediaPlayerAdapter(this, new MediaPlayerListener());

        mediaNotificationManager = new MediaNotificationManager(this);
        mediaBrowserHelper = new MediaBrowserHelper(this,MediaService.class);
        mediaBrowserHelper.setPlayerController(this);
    }


    //THIS IS SO IMPORTANT FOR THE NOTIFICATION AND MEDIA BUTTONS
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat,intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        playback.stop();
        stopSelf();
    }

    public void setTrackSpeed(float speed){
        playback.setSpeed(speed);
    }

    @Override
    public void speedUp(float speed) {
        setTrackSpeed(speed);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.d(TAG, "onGetRoot: CALLED" + clientPackageName);
        if (clientPackageName.equals(getApplicationContext().getPackageName())) {
            return new BrowserRoot("some_real_playlist", null);
        }
        return new BrowserRoot("none", null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "onLoadChildren: CALLED" + parentId + ", " + result);
        if (parentId.equals("none")) {
            result.sendResult(null);
            return;
        }
        result.sendResult(myApplication.getMediaItems());
    }


    public class MediaSessionCallback extends MediaSessionCompat.Callback {

        private List<MediaSessionCompat.QueueItem> playlist = new ArrayList<>();
        private int queueIndex = -1;
        private MediaMetadataCompat preparedMedia;

        private void resetPlaylist() {
            playlist.clear();
            queueIndex = -1;
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            Log.d(TAG, "onCustomAction: CALLED");
            if(action.equals(CUSTOM_ACTION_SPEED)){
                float speed = extras.getFloat(SPEED_VALUE,0);
                if(preparedMedia != null){
                    playback.setSpeed(speed);
                }
            }
            if(action.equals(INCREASE_15)){
                playback.increase15Seconds();
            }
            if(action.equals(DECREASE_15)){
                playback.decrease15Seconds();
            }
        }


        @Override
        public void onPrepare() {
            if (queueIndex < 0 && playlist.isEmpty()) {
                return;
                //nothing to play
            }
            String mediaId = playlist.get(queueIndex).getDescription().getMediaId();
            //preparedMedia = mediaLibrary.getTreeMap().get(mediaId);
            preparedMedia = myApplication.getTreeMap().get(mediaId);
            Log.d(TAG, "onPrepare: prepared media is :" + preparedMedia);
            mediaSessionCompat.setMetadata(preparedMedia);
            if (!mediaSessionCompat.isActive()) {
                mediaSessionCompat.setActive(true);
            }
        }

        @Override
        public void onPlay() {
            Log.d(TAG, "onPlay: CALLED");
            Log.d(TAG, "onPlay: QUEUE INDEX :" + queueIndex);

            if (!isReadyToPlay()) {
                return;
            }
            if (preparedMedia == null) {
                onPrepare();
            }

            playback.playFromMedia(preparedMedia);
            playback.setSpeed(getSavedSpeed());
            myPreferenceManager.saveQueuePosition(queueIndex);
            myPreferenceManager.saveLastPlayedMedia(preparedMedia.getDescription().getMediaId());
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {

            if (extras.getBoolean(QUEUE_NEW_PLAYLIST, false)) {
                resetPlaylist();
            }

            Log.d(TAG, "onPlayFromMediaId: Called" + mediaId);
            preparedMedia = myApplication.getTreeMap().get(mediaId);
            mediaSessionCompat.setMetadata(preparedMedia);
            if (!mediaSessionCompat.isActive()) {
                mediaSessionCompat.setActive(true);
            }
            playback.playFromMedia(preparedMedia);
            playback.setSpeed(getSavedSpeed());

            int newQueuePosition = extras.getInt(MEDIA_QUEUE_POSITION, -1);
            if (newQueuePosition == -1) {
                queueIndex++;
            } else {
                queueIndex = newQueuePosition;
            }
            myPreferenceManager.saveQueuePosition(queueIndex);
            myPreferenceManager.saveLastPlayedMedia(preparedMedia.getDescription().getMediaId());
        }

        @Override
        public void onPause() {
            playback.pause();
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "onSkipToNext: skipping to next called");
            queueIndex = (++queueIndex % playlist.size());
            preparedMedia = null;
            onPlay();
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious: skipping to previous called");
            queueIndex = (queueIndex > 0) ? queueIndex - 1 : playlist.size() - 1;
            preparedMedia = null;
            onPlay();
        }

        @Override
        public void onFastForward() {
            playback.increase15Seconds();
        }

        @Override
        public void onStop() {
            playback.stop();
            mediaSessionCompat.setActive(false);
        }

        @Override
        public void onSeekTo(long pos) {
            playback.seekTo(pos);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            if(myPreferenceManager.isFirstLaunch()){
                Log.d(TAG, "onAddQueueItem : CALLED : position in list" + playlist.size());
                playlist.add(new MediaSessionCompat.QueueItem(description, description.hashCode()));
                queueIndex = (queueIndex == -1) ? 0 : queueIndex;
                mediaSessionCompat.setQueue(playlist);
            }
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            Log.d(TAG, "onRemoveQueueItem: Called");
            playlist.remove(new MediaSessionCompat.QueueItem(description, description.hashCode()));
            queueIndex = (playlist.isEmpty()) ? -1 : queueIndex;
            mediaSessionCompat.setQueue(playlist);
        }

        private boolean isReadyToPlay() {
            return !(playlist.isEmpty());
        }

        private Float getSavedSpeed() {
            switch (myPreferenceManager.getSpeedIndex()) {
                case 0:
                    return .75f;
                case 1:
                    return 1.0f;
                case 2:
                    return 1.25f;
                case 3:
                    return 1.5f;
                case 4:

                    return 2.0f;
                case 5:
                    return 3.0f;
                default:
                    return 1.0f;
            }
        }
    }

    private class MediaPlayerListener implements PlaybackInfoListener {

        private ServiceManager serviceManager;

        public MediaPlayerListener() {
            serviceManager = new ServiceManager();
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            mediaSessionCompat.setPlaybackState(state);
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING:
                    serviceManager.displayNotification(state);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    serviceManager.displayNotification(state);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    serviceManager.moveServiceOutOfStartedState();
                    break;

            }
        }

        @Override
        public void onPlaybackComplete() {
            Log.d(TAG, "onPlaybackComplete: CALLED");
            mediaSessionCompat.getController().getTransportControls().skipToNext();
        }




        @Override
        public void onSeekTo(long progress, long max) {
            Intent intent = new Intent();
            intent.setAction(getString(R.string.broadcast_seekbar_update));
            intent.putExtra(SEEK_BAR_PROGRESS, progress);
            intent.putExtra(SEEK_BAR_MAX, max);
            sendBroadcast(intent);
        }

        @Override
        public void updateUI(String mediaId) {
            Intent intent = new Intent();
            intent.setAction(getString(R.string.broadcast_update_ui));
            intent.putExtra(getString(R.string.broadcast_new_media_id), mediaId);
            sendBroadcast(intent);
        }


        //this class is fo handling to display and update the notification every time there is a change in the playback state
        class ServiceManager {

            public ServiceManager() {
            }

            private PlaybackStateCompat state;

            public void displayNotification(PlaybackStateCompat state) {
                Notification notification = null;
                switch (state.getState()) {

                    case PlaybackStateCompat.STATE_PLAYING: {
                        notification = mediaNotificationManager.buildNotification(state,
                                getSessionToken(), playback.getCurrentMedia().getDescription(), null);
                        if (!isServiceRunning) {
                            ContextCompat.startForegroundService(
                                    MediaService.this,
                                    new Intent(MediaService.this, MediaService.class));
                            isServiceRunning = true;
                        }
                        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;
                    }

                    case PlaybackStateCompat.STATE_PAUSED:
                        stopForeground(false);
                        notification =
                                mediaNotificationManager.buildNotification(
                                        state, getSessionToken(), playback.getCurrentMedia().getDescription(), null);
                        mediaNotificationManager.getNotificationManager()
                                .notify(MediaNotificationManager.NOTIFICATION_ID, notification);
                        break;
                }
            }

            private void moveServiceOutOfStartedState() {
                stopForeground(true);
                stopSelf();
                isServiceRunning = false;
            }
        }
    }

}
