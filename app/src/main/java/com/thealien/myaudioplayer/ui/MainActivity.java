package com.thealien.myaudioplayer.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import android.animation.Animator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.offline.ProgressiveDownloadAction;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.thealien.myaudioplayer.R;
import com.thealien.myaudioplayer.client.MediaBrowserHelper;
import com.thealien.myaudioplayer.client.MediaBrowserHelperCallback;
import com.thealien.myaudioplayer.player.MediaPlayerAdapter;
import com.thealien.myaudioplayer.player.PlayerAdapter;
import com.thealien.myaudioplayer.player.PlayerController;
import com.thealien.myaudioplayer.services.MediaDownloadService;
import com.thealien.myaudioplayer.services.MediaService;
import com.thealien.myaudioplayer.util.MediaSeekBar;
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

public class MainActivity extends AppCompatActivity implements IMainActivity, MediaBrowserHelperCallback, View.OnClickListener {

    private static final String TAG = "MainActivity";

    public static boolean firstTime = false;

    //UI Components
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private LinearLayout oralPathologyLayout, prosthesisLayout, crownLayout, operativeLayout, orthoLayout, pedoLayout, pharmaLayout;
    private RelativeLayout bottomMediaControllerLayout;
    private RelativeLayout fullPanelLayout;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageView playPauseImage, fullPanelPlayPause, fullPanelSkipToNext, fullPanelSkipToPrevious;
    private TextView titleTextView, fullPanelTextTitle, fullPanelTextDescription;
    //private MediaSeekBar seekBar;
    private MediaSeekBar fullPanelSeekBar;
    private ProgressBar horizontalProgressBar;
    private ImageView skip15ToPrevious, skip15ToNext, downloadMediaImage;
    private ImageView speedImageView;
    private Chronometer trackingChronometer;
    private Chronometer fullChronometer;

    private LottieAnimationView downloadAnimation;

    //Vars
    private MediaBrowserHelper mediaBrowserHelper;
    private boolean isPlaying;
    private MyApplication myApplication;
    private MyPreferenceManager myPreferenceManager;
    private SeekBarBroadCastReceiver seekBarBroadCastReceiver;
    private UpdateUIBroadCastReceiver updateUIBroadCastReceiver;
    private MediaMetadataCompat selectedMedia;
    private FirebaseFirestore firestore;
    private boolean onAppOpen;
    private boolean wasConfigurationChanged;

    private long seekBarProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();

        firestore = FirebaseFirestore.getInstance();
        firstTime = true;

        myPreferenceManager = MyPreferenceManager.getInstance(this);
        myPreferenceManager.saveFirstLaunch(true);
        if (savedInstanceState == null) {
            loadMediaBaseFragment(MediaBaseFragment.newInstance("Crown"));
        } else {
            selectedMedia = savedInstanceState.getParcelable("selected_media");
            isPlaying = savedInstanceState.getBoolean("is_playing");
            if (selectedMedia != null) {
                titleTextView.setText(selectedMedia.getDescription().getTitle());

                setIsPlaying(isPlaying);
            }
        }
        myApplication = new MyApplication();
        mediaBrowserHelper = new MediaBrowserHelper(this, MediaService.class);
        mediaBrowserHelper.setMediaBrowserHelperCallback(this);
        Log.d(TAG, "onCreate: saved playlist id is :" + myPreferenceManager.getPlaylistId());
        //initSpinner();

    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("selected_media", selectedMedia);
        outState.putBoolean("is_playing", isPlaying);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        wasConfigurationChanged = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: CALLED");
        if (firstTime) {
            if (!getMyPreferenceManager().getPlaylistId().equals("")) {
                prepareLastPlayedMedia();
            } else {
                if (myPreferenceManager.isFirstLaunch()) {
                    mediaBrowserHelper.onStart(wasConfigurationChanged, myPreferenceManager.isFirstLaunch());
                    //initSpinner();
                }
            }
        }
        setIsPlaying(isPlaying);
    }


    private void prepareLastPlayedMedia() {

        final List<MediaMetadataCompat> mediaList = new ArrayList<>();

        Query query = firestore.collection("Subjects/")
                .document(myPreferenceManager.getLastSubject())
                .collection("records/");

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        MediaMetadataCompat media = toMediaItem(document);
                        mediaList.add(media);
                        if (media.getDescription().getMediaId().equals(myPreferenceManager.getLastPlayedMedia())) {
                            titleTextView.setText(media.getDescription().getTitle());
                            fullPanelTextTitle.setText(media.getDescription().getTitle());
                            fullPanelTextDescription.setText(media.getDescription().getSubtitle());
                            updateTrackingChronometer();
                            fullPanelSeekBar.setProgress(myPreferenceManager.getSeekBarPosition());
                            //horizontalProgressBar.setProgress(myPreferenceManager.getSeekBarPosition());
                        }
                    }
                } else {
                    Log.d(TAG, "onComplete: error getting documents: " + task.getException());
                }
                //mediaBrowserHelper.getTransportControls().seekTo(myPreferenceManager.getQueuePosition());
                onFinishedGettingPreviousSessionData(mediaList);
            }
        });
    }

    private void onFinishedGettingPreviousSessionData(List<MediaMetadataCompat> mediaList) {
        myApplication.setMediaItems(mediaList);
        mediaBrowserHelper.onStart(wasConfigurationChanged, myPreferenceManager.isFirstLaunch());
    }

    private MediaMetadataCompat toMediaItem(QueryDocumentSnapshot document) {
        MediaMetadataCompat media = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, document.getId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, document.get("title").toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, document.get("description").toString())
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, document.get("media_url").toString())
                .build();
        return media;
    }

    @Override
    protected void onResume() {
        super.onResume();
        initSeekBarBroadCast();
        initUpdateUIBroadCast();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: CALLED");
        if (!getMyPreferenceManager().getPlaylistId().equals("")) {
            prepareLastPlayedMedia();
        } else {
            if (myPreferenceManager.isFirstLaunch()) {
                mediaBrowserHelper.onStart(wasConfigurationChanged, myPreferenceManager.isFirstLaunch());
                //initSpinner();
            }
        }
        Log.d(TAG, "onStart: CALLED");
        setIsPlaying(isPlaying);
        firstTime = false;
        if (seekBarBroadCastReceiver != null) {
            unregisterReceiver(seekBarBroadCastReceiver);
        }
        if (updateUIBroadCastReceiver != null) {
            unregisterReceiver(updateUIBroadCastReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: CALLED");
        myPreferenceManager.saveFirstLaunch(false);

        firstTime = false;
    }


    @Override
    public void showProgress() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void playPause() {

    }

    @Override
    public void onMediaControllerConnected(MediaControllerCompat mediaController) {
        //seekBar.setEnabled(false);
        //seekBar.setMediaController(mediaController);
        fullPanelSeekBar.setMediaController(mediaController);
    }

    @Override
    public MyApplication getMyApplication() {
        return myApplication;
    }

    @Override
    public MyPreferenceManager getMyPreferenceManager() {
        return myPreferenceManager;
    }

    @Override
    public void onMediaSelected(String playlistId, MediaMetadataCompat mediaItem, int queuePosition) {
        if (mediaItem != null) {
            Log.d(TAG, "onMediaSelected: Called :" + mediaItem.getDescription().getTitle());

            String currentPlaylistId = myPreferenceManager.getPlaylistId();

            Bundle bundle = new Bundle();
            bundle.putInt(MEDIA_QUEUE_POSITION, queuePosition);
            if (currentPlaylistId.equals(playlistId) && !firstTime) {
                mediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
            } else {
                bundle.putBoolean(QUEUE_NEW_PLAYLIST, true);
                Log.d(TAG, "onMediaSelected: playlist id is :" + playlistId);
                mediaBrowserHelper.subscribeToNewPlaylist(playlistId);
                mediaBrowserHelper.getTransportControls().playFromMediaId(mediaItem.getDescription().getMediaId(), bundle);
                firstTime = false;
            }
            //because this means that the user has selected something to play
            onAppOpen = true;
        } else {
            Toast.makeText(this, "selected something to play", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Toolbar getToolBar() {
        return toolbar;
    }


    @Override
    public void onPlaybackStateChanged(PlaybackStateCompat state) {
        Log.d(TAG, "onPlaybackStateChanged: CALLED");
        isPlaying = (state != null) && (state.getState() == PlaybackStateCompat.STATE_PLAYING);

        setIsPlaying(isPlaying);
    }

    @Override
    public void onMetadataChanged(MediaMetadataCompat metaData) {
        selectedMedia = metaData;
        titleTextView.setText(metaData.getDescription().getTitle());
        fullPanelTextTitle.setText(metaData.getDescription().getTitle());
        fullPanelTextDescription.setText(metaData.getDescription().getSubtitle());
        fullChronometer.setBase(myPreferenceManager.getChronometerDuration());
    }

    public void setIsPlaying(boolean isPlaying) {
        if (isPlaying) {
            Glide.with(getApplicationContext())
                    .load(R.drawable.ic_pause)
                    .into(playPauseImage);
            Glide.with(getApplicationContext())
                    .load(R.drawable.icons8_pause_64)
                    .into(fullPanelPlayPause);
        } else {
            Glide.with(getApplicationContext())
                    .load(R.drawable.ic_play)
                    .into(playPauseImage);
            Glide.with(getApplicationContext())
                    .load(R.drawable.icons8_play_64)
                    .into(fullPanelPlayPause);
        }
        this.isPlaying = isPlaying;
    }

    //TODO check this if there is an error
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //myPreferenceManager.setPlaylistId(null);
        fullPanelSeekBar.disconnectController();
        mediaBrowserHelper.onStop();
        Log.d(TAG, "onDestroy: Called  " + myPreferenceManager.getPlaylistId());
    }

    private void updateTrackingChronometer() {
        fullChronometer.setBase(myPreferenceManager.getChronometerDuration());
    }

    private class SeekBarBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            seekBarProgress = intent.getLongExtra(SEEK_BAR_PROGRESS, 0);
            long max = intent.getLongExtra(SEEK_BAR_MAX, 0);
            myPreferenceManager.saveChronometerDuration(SystemClock.elapsedRealtime() - max);
            if (!fullPanelSeekBar.isTracking()) {
                horizontalProgressBar.setProgress((int) seekBarProgress);
                horizontalProgressBar.setMax((int) max);
                fullPanelSeekBar.setProgress((int) seekBarProgress);
                fullPanelSeekBar.setMax((int) max);
                myPreferenceManager.saveSeekBarPosition((int) seekBarProgress);
                fullChronometer.setBase(SystemClock.elapsedRealtime() - max);
            }
            trackingChronometer.setBase(SystemClock.elapsedRealtime() - seekBarProgress);
            // myPreferenceManager.saveSeekBarPosition((int)progress);
            //Log.d(TAG, "onReceive: seek bar position" + myPreferenceManager.getSeekBarPosition());
        }
    }

    private class UpdateUIBroadCastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String newMediaId = intent.getStringExtra(getString(R.string.broadcast_new_media_id));
            Log.d(TAG, "onReceive: CALLED  " + newMediaId);
            if (getMediaBaseFragment() != null) {
                Log.d(TAG, "onReceive: " + myApplication.getMediaItem(newMediaId).getDescription().getMediaId());
                getMediaBaseFragment().updateUI(myApplication.getMediaItem(newMediaId));
            }
        }
    }

    private void initUpdateUIBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_update_ui));
        updateUIBroadCastReceiver = new UpdateUIBroadCastReceiver();
        registerReceiver(updateUIBroadCastReceiver, intentFilter);
    }

    private void initSeekBarBroadCast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(getString(R.string.broadcast_seekbar_update));
        seekBarBroadCastReceiver = new SeekBarBroadCastReceiver();
        registerReceiver(seekBarBroadCastReceiver, intentFilter);
    }

    private void downloadMedia() {
        downloadAnimation.playAnimation();
        downloadAnimation.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {

            }

            @Override
            public void onAnimationEnd(Animator animator) {
                downloadAnimation.setProgress(1.0f);
            }

            @Override
            public void onAnimationCancel(Animator animator) {

            }

            @Override
            public void onAnimationRepeat(Animator animator) {

            }
        });
        ProgressiveDownloadAction progressiveDownloadAction = new ProgressiveDownloadAction(selectedMedia.getDescription().getMediaUri(),
                false, null, null);
        DownloadService.startWithAction(this, MediaDownloadService.class, progressiveDownloadAction, false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Oral_Pathology:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Oral Pathology"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Crown:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Crown"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Prothesis:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Prosthesis"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Pharma:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Pharma"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Pedo:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Pedo"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Operative:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Operative"));
                drawerLayout.closeDrawers();
                return;
            case R.id.Ortho:
                loadMediaBaseFragment(MediaBaseFragment.newInstance("Ortho"));
                drawerLayout.closeDrawers();
                return;
            case R.id.play_pause_image_view:
                playPauseToggle();
                return;
            case R.id.play_pause_full_panel:
                playPauseToggle();
                return;
            case R.id.full_panel_skip_to_next:
                mediaBrowserHelper.getTransportControls().skipToNext();
                return;
            case R.id.full_panel_skip_to_previous:
                mediaBrowserHelper.getTransportControls().skipToPrevious();
                return;
            case R.id.skip_15_to_next_image:
                mediaBrowserHelper.getTransportControls().sendCustomAction(INCREASE_15, new Bundle());
                return;
            case R.id.skip_15_to_previous_image:
                mediaBrowserHelper.getTransportControls().sendCustomAction(DECREASE_15, new Bundle());
                return;
            case R.id.download_could_animation:
                downloadMedia();
                return;
            case R.id.speed_image_view:
                initPopupMenu(view);
                return;
        }
    }

    private void initPopupMenu(View view) {
        final Bundle bundle = new Bundle();
        final PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.getMenu().getItem(myPreferenceManager.getSpeedIndex()).setChecked(true);
        Log.d(TAG, "initPopupMenu: speed index is :" + myPreferenceManager.getSpeedIndex());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                Toast.makeText(MainActivity.this, menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                //popupMenu.getMenu().getItem(1).setChecked(false);
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.speed__75:
                        myPreferenceManager.setPlaybackSpeedIndex(0);
                        bundle.putFloat(SPEED_VALUE, .75f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;
                    case R.id.speed_1:
                        myPreferenceManager.setPlaybackSpeedIndex(1);
                        bundle.putFloat(SPEED_VALUE, 1.0f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;
                    case R.id.speed_1_25:
                        myPreferenceManager.setPlaybackSpeedIndex(2);
                        bundle.putFloat(SPEED_VALUE, 1.25f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;
                    case R.id.speed_1_5:
                        myPreferenceManager.setPlaybackSpeedIndex(3);
                        bundle.putFloat(SPEED_VALUE, 1.5f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;
                    case R.id.speed_2:
                        myPreferenceManager.setPlaybackSpeedIndex(4);
                        bundle.putFloat(SPEED_VALUE, 2.0f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;
                    case R.id.speed_3:
                        myPreferenceManager.setPlaybackSpeedIndex(5);
                        bundle.putFloat(SPEED_VALUE, 3.0f);
                        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
                        break;

                }
                //myPreferenceManager.setPlaybackSpeedIndex(menuItem.getOrder());
                return true;
            }
        });
        //DON"T forget this
        popupMenu.show();
        Log.d(TAG, "initPopupMenu: speed index is :" + myPreferenceManager.getSpeedIndex());
    }


    private void playPauseToggle() {

        // mediaBrowserHelper.getTransportControls().play();
        Log.d(TAG, "playPause: onAppOpen is :" + onAppOpen);
        if (onAppOpen) {
            if (isPlaying) {
                mediaBrowserHelper.getTransportControls().pause();
            } else {
                mediaBrowserHelper.getTransportControls().play();
                speedMediaUp();
                isPlaying = false;
            }
        } else {
            if (!myPreferenceManager.getPlaylistId().equals("")) {
                onMediaSelected(myPreferenceManager.getPlaylistId(),
                        myApplication.getMediaItem(myPreferenceManager.getLastPlayedMedia()),
                        myPreferenceManager.getQueuePosition());
                //seekBar.setProgress(myPreferenceManager.getSeekBarPosition());
                speedMediaUp();
                Toast.makeText(this, "the index is :" + myPreferenceManager.getQueuePosition(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Select something to play", Toast.LENGTH_SHORT).show();
            }
            isPlaying = true;
        }
    }

    private void speedMediaUp() {
        Bundle bundle = new Bundle();
        bundle.putFloat(SPEED_VALUE, getSavedSpeed());
        Toast.makeText(this, "speed is " + getSavedSpeed(), Toast.LENGTH_SHORT).show();
        mediaBrowserHelper.getTransportControls().sendCustomAction(CUSTOM_ACTION_SPEED, bundle);
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

    @Override
    public void onBackPressed() {
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ||
                slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private MediaBaseFragment getMediaBaseFragment() {
        MediaBaseFragment mediaBaseFragment = (MediaBaseFragment) getSupportFragmentManager()
                .findFragmentByTag(getString(R.string.fragment_base_media));
        if (mediaBaseFragment != null) {
            return mediaBaseFragment;
        }
        return null;
    }

    private void loadMediaBaseFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment, getString(R.string.fragment_base_media))
                .commit();
    }

    private void initUI() {
        fullChronometer = findViewById(R.id.chronometer_full_duration);
        trackingChronometer = findViewById(R.id.chronometer_current_position);
        speedImageView = findViewById(R.id.speed_image_view);
        speedImageView.setOnClickListener(this);
        downloadAnimation = findViewById(R.id.download_could_animation);
        downloadAnimation.setOnClickListener(this);
        //downloadMediaImage = findViewById(R.id.download_media_image);
//        downloadMediaImage.setOnClickListener(this);
        skip15ToNext = findViewById(R.id.skip_15_to_next_image);
        skip15ToPrevious = findViewById(R.id.skip_15_to_previous_image);
        skip15ToPrevious.setOnClickListener(this);
        skip15ToNext.setOnClickListener(this);
        horizontalProgressBar = findViewById(R.id.horizontal_progress_bar);
        fullPanelSeekBar = findViewById(R.id.full_panel_seek_bar);
        //seekBar = findViewById(R.id.bottom_panel_seek_bar);
        fullPanelSkipToPrevious = findViewById(R.id.full_panel_skip_to_previous);
        fullPanelSkipToNext = findViewById(R.id.full_panel_skip_to_next);
        fullPanelSkipToNext.setOnClickListener(this);
        fullPanelSkipToPrevious.setOnClickListener(this);
        fullPanelPlayPause = findViewById(R.id.play_pause_full_panel);
        fullPanelPlayPause.setOnClickListener(this);
        fullPanelTextTitle = findViewById(R.id.full_panel_text_title);
        fullPanelTextDescription = findViewById(R.id.full_panel_text_description);
        playPauseImage = findViewById(R.id.play_pause_image_view);
        playPauseImage.setOnClickListener(this);
        titleTextView = findViewById(R.id.media_song_title_text_view);
        fullPanelLayout = findViewById(R.id.full_panel);
        bottomMediaControllerLayout = findViewById(R.id.bottom_panel);
        slidingUpPanelLayout = findViewById(R.id.main_sliding_up_panel);
        progressBar = findViewById(R.id.progress_bar);
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.main_tool_bar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_opne, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        oralPathologyLayout = findViewById(R.id.Oral_Pathology);
        prosthesisLayout = findViewById(R.id.Prothesis);
        crownLayout = findViewById(R.id.Crown);
        operativeLayout = findViewById(R.id.Operative);
        orthoLayout = findViewById(R.id.Ortho);
        pedoLayout = findViewById(R.id.Pedo);
        pharmaLayout = findViewById(R.id.Pharma);
        oralPathologyLayout.setOnClickListener(this);
        prosthesisLayout.setOnClickListener(this);
        crownLayout.setOnClickListener(this);
        orthoLayout.setOnClickListener(this);
        pedoLayout.setOnClickListener(this);
        pharmaLayout.setOnClickListener(this);
        operativeLayout.setOnClickListener(this);


        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                bottomMediaControllerLayout.setAlpha(1 - slideOffset);
                fullPanelLayout.setAlpha(slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    bottomMediaControllerLayout.setVisibility(View.GONE);
                } else {
                    bottomMediaControllerLayout.setVisibility(View.VISIBLE);
                }
            }
        });
        //slidingUpPanelLayout.setDragView(R.id.bottom_panel);
        //initSpinner();
    }
}
