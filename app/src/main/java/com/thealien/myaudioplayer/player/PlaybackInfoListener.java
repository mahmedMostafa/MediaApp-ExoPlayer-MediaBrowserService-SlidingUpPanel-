package com.thealien.myaudioplayer.player;

import android.support.v4.media.session.PlaybackStateCompat;

public interface PlaybackInfoListener {

     void onPlaybackStateChanged(PlaybackStateCompat state);

     void onPlaybackComplete();

     void onSeekTo(long progress, long max);

     void updateUI(String mediaId);

}
