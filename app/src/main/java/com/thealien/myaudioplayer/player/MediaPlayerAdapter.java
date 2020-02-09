package com.thealien.myaudioplayer.player;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.thealien.myaudioplayer.R;
import com.thealien.myaudioplayer.client.MediaBrowserHelper;
import com.thealien.myaudioplayer.ui.MainActivity;
import com.thealien.myaudioplayer.util.DownloadUtil;

import static com.thealien.myaudioplayer.util.Constants.CUSTOM_ACTION_SPEED;
import static com.thealien.myaudioplayer.util.Constants.DECREASE_15;
import static com.thealien.myaudioplayer.util.Constants.INCREASE_15;

public class MediaPlayerAdapter extends PlayerAdapter implements PlayerController{


    private static final String TAG = "MediaPlayerAdapter";

    private final Context context;
    private MediaMetadataCompat currentMedia;
    private int currentState;
    private long startTime;
    //a boolean to know if it has completed playing the current media or not
    private boolean mCurrentMediaPlayedToCompletion;
    private PlaybackInfoListener playbackInfoListener;
    private ExoPlayerEventListener exoPlayerEventListener;

    private MediaBrowserHelper mediaBrowserHelper;

    //exoPlayer objects
    private SimpleExoPlayer exoPlayer;
    private TrackSelector trackSelector;
    private DefaultRenderersFactory defaultRenderersFactory;
    private DataSource.Factory dataSourceFactory;
    private CacheDataSourceFactory cacheDataSourceFactory;

    public MediaPlayerAdapter(@NonNull Context context, PlaybackInfoListener listener) {
        super(context);
        this.context = context.getApplicationContext();
        playbackInfoListener = listener;
    }
    //this method is for initializing everything related to the exoPlayer
    private void initExoPlayer() {
        trackSelector = new DefaultTrackSelector();
        defaultRenderersFactory = new DefaultRenderersFactory(context);
        dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "AudioStreamer"));
        cacheDataSourceFactory = new CacheDataSourceFactory(DownloadUtil.getCache(context),dataSourceFactory);
        exoPlayer = ExoPlayerFactory.newSimpleInstance(defaultRenderersFactory, trackSelector, new DefaultLoadControl());
//        PlaybackParameters param = new PlaybackParameters(2.0f);
//        //param.speed(1f);// 1f is 1x, 2f is 2x
//        exoPlayer.setPlaybackParameters(param);
        if (exoPlayerEventListener == null) {
            exoPlayerEventListener = new ExoPlayerEventListener();
        }
        exoPlayer.addListener(exoPlayerEventListener);
    }

    private void release() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    protected void onPlay() {
        Log.d(TAG, "onPlay: called");
        if (exoPlayer != null && !exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(true);
            setNewState(PlaybackStateCompat.STATE_PLAYING);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        if (exoPlayer != null && exoPlayer.getPlayWhenReady()) {
            exoPlayer.setPlayWhenReady(false);
            setNewState(PlaybackStateCompat.STATE_PAUSED);
        }
    }

    @Override
    public void playFromMedia(MediaMetadataCompat metadata) {
        playFile(metadata);
        startTrackingPlayback();
        Log.d(TAG, "playFromMedia: called");
    }


    @Override
    public MediaMetadataCompat getCurrentMedia() {
        return currentMedia;
    }

    @Override
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.getPlayWhenReady();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: called");
        setNewState(PlaybackStateCompat.STATE_STOPPED);
        release();
    }

    @Override
    public void seekTo(long position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position);
            // Set the state (to the current state) because the position changed and should
            // be reported to clients.
            setNewState(currentState);
        }
    }

    @Override
    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(volume);
        }
    }

    @Override
    public void setSpeed(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        exoPlayer.setPlaybackParameters(parameters);
    }

    @Override
    public void increase15Seconds() {
        exoPlayer.seekTo(exoPlayer.getContentPosition()+15000);
    }

    @Override
    public void decrease15Seconds() {
        if(exoPlayer.getContentPosition() <= 15000){
            exoPlayer.seekTo(0);
        }else{
            exoPlayer.seekTo(exoPlayer.getContentPosition() - 15000);
        }
    }

    @Override
    public long getDuration() {
        return exoPlayer.getDuration();
    }

    private void startTrackingPlayback() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying()) {
                    playbackInfoListener.onSeekTo(exoPlayer.getContentPosition(), exoPlayer.getDuration());
                    handler.postDelayed(this, 100);
                }
                //the getDuration method returns a negative one when the audio is stopped
                if (exoPlayer.getContentPosition() >= exoPlayer.getDuration()
                        && exoPlayer.getDuration() > 0) {
                    playbackInfoListener.onPlaybackComplete();
                }
            }
        };
        handler.postDelayed(runnable, 100);
    }

    private void playFile(MediaMetadataCompat metaData) {
        String mediaId = metaData.getDescription().getMediaId();
        boolean mediaChanged = (currentMedia == null) || !mediaId.equals(currentMedia.getDescription().getMediaId());
        if (mCurrentMediaPlayedToCompletion) {
            mediaChanged = true;
            mCurrentMediaPlayedToCompletion = false;
        }
        if (!mediaChanged) {
            if (!isPlaying()) {
                play();

            }
            return;
        } else {
            release();
        }
        currentMedia = metaData;

        initExoPlayer();

        //make a try catch block as sometimes the audio can't load the media from the internet
        try {
            MediaSource audioSource =
                    new ExtractorMediaSource.Factory(cacheDataSourceFactory)
                            .createMediaSource(Uri.parse(metaData.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)));
            exoPlayer.prepare(audioSource);
            Log.d(TAG, "onPlayerStateChanged: PREPARE");

        } catch (Exception e) {
            throw new RuntimeException("Failed to play media uri: "
                    + metaData.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI), e);
        }

        play();

        Log.d(TAG, "playFile: play ya nigm");

    }

    // This is the main reducer for the player state machine.
    private void setNewState(@PlaybackStateCompat.State int newPlayerState) {
        Log.d(TAG, "setNewState: Called");
        currentState = newPlayerState;

        if (currentState == PlaybackStateCompat.STATE_STOPPED) {
            mCurrentMediaPlayedToCompletion = true;
        }

        long reportTime = exoPlayer == null ? 0 : exoPlayer.getCurrentPosition();
        publicStateBuilder(reportTime);
    }

    //this is the method that sends the state to the media session
    private void publicStateBuilder(long reportTime) {
        Log.d(TAG, "publicStateBuilder: Called");
        final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions())
                .setState(currentState, reportTime, 1.0f, SystemClock.elapsedRealtime());
        setSpeedCustomAction(stateBuilder);
        playbackInfoListener.onPlaybackStateChanged(stateBuilder.build());
        playbackInfoListener.updateUI(currentMedia.getDescription().getMediaId());
    }

    private void setSpeedCustomAction(PlaybackStateCompat.Builder stateBuilder){
        //for speeding up the track
        Bundle extra = new Bundle();
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                CUSTOM_ACTION_SPEED, context.getResources().getString(R.string.custom_action),R.mipmap.ic_launcher
        ).setExtras(extra)
                .build());

        //for increasing 15 seconds to the track
        Bundle bundle = new Bundle();
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                INCREASE_15, context.getResources().getString(R.string.custom_action_increase_15),R.mipmap.ic_launcher
        ).setExtras(bundle)
                .build());

        //for decreasing 15 seconds to the track
        Bundle b = new Bundle();
        stateBuilder.addCustomAction(new PlaybackStateCompat.CustomAction.Builder(
                DECREASE_15, context.getResources().getString(R.string.custom_action_decrease_15),R.mipmap.ic_launcher
        ).setExtras(b)
                .build());
    }

    /**
     * Set the current capabilities available on this session. Note: If a capability is not
     * listed in the bitmask of capabilities then the MediaSession will not handle it. For
     * example, if you don't want ACTION_STOP to be handled by the MediaSession, then don't
     * included it in the bitmask that's returned.
     */
    @PlaybackStateCompat.Actions
    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        switch (currentState) {
            case PlaybackStateCompat.STATE_STOPPED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PAUSE;
                break;
            case PlaybackStateCompat.STATE_PLAYING:
                actions |= PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE
                        | PlaybackStateCompat.ACTION_SEEK_TO;
                break;
            case PlaybackStateCompat.STATE_PAUSED:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_STOP;
                break;
            default:
                actions |= PlaybackStateCompat.ACTION_PLAY
                        | PlaybackStateCompat.ACTION_PLAY_PAUSE
                        | PlaybackStateCompat.ACTION_STOP
                        | PlaybackStateCompat.ACTION_PAUSE;
        }
        return actions;
    }

    @Override
    public void speedUp(float speed) {
        PlaybackParameters parameters = new PlaybackParameters(speed);
        exoPlayer.setPlaybackParameters(parameters);
    }


    private class ExoPlayerEventListener implements Player.EventListener {

        @Override
        public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

        }

        @Override
        public void onLoadingChanged(boolean isLoading) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {

                case Player.STATE_BUFFERING: {
                    Log.d(TAG, "onPlayerStateChanged: BUFFERING");
                    startTime = System.currentTimeMillis();
                    break;
                }
                case Player.STATE_ENDED: {
                    setNewState(PlaybackStateCompat.STATE_PAUSED);
                    break;
                }
                case Player.STATE_READY: {
                    Log.d(TAG, "onPlayerStateChanged: READY");
                    Log.d(TAG, "onPlayerStateChanged: TIME ELAPSED: " + (System.currentTimeMillis() - startTime));
                    break;
                }
                case Player.STATE_IDLE: {
                    Log.d(TAG, "onPlayerStateChanged: IDLE state called");
                    break;
                }
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {

        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {

        }

        @Override
        public void onPositionDiscontinuity(int reason) {

        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

        }

        @Override
        public void onSeekProcessed() {

        }
    }
}
