package com.thealien.myaudioplayer.ui;

import android.support.v4.media.MediaMetadataCompat;

import androidx.appcompat.widget.Toolbar;

import com.thealien.myaudioplayer.util.MyApplication;
import com.thealien.myaudioplayer.util.MyPreferenceManager;

public interface IMainActivity {

    void showProgress();
    void hideProgress();
    void playPause();

    MyApplication getMyApplication();

    MyPreferenceManager getMyPreferenceManager();

    void onMediaSelected(String playlistId , MediaMetadataCompat mediaItem,int queuePosition);

    Toolbar getToolBar();
}
