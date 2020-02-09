package com.thealien.myaudioplayer.client;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

public interface MediaBrowserHelperCallback {

    void onPlaybackStateChanged(PlaybackStateCompat state);

    void onMetadataChanged(MediaMetadataCompat metaData);

    void onMediaControllerConnected(MediaControllerCompat mediaController);
}
