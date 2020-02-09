package com.thealien.myaudioplayer.ui;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.thealien.myaudioplayer.R;
import com.thealien.myaudioplayer.adapters.MediaRecyclerAdapter;
import com.thealien.myaudioplayer.models.MediaItem;

import java.util.ArrayList;
import java.util.List;

import static com.thealien.myaudioplayer.util.Constants.KEY_SELECTED_SUBJECT;

public class MediaBaseFragment extends Fragment implements MediaRecyclerAdapter.OnItemClickListener {

    private static final String TAG = "MediaBaseFragment";
    //Views
    private View view;
    private IMainActivity iMainActivity;
    private RecyclerView recyclerView;
    private TextView offlineTextView;

    //Vars

    private MediaRecyclerAdapter adapter;
    private List<MediaMetadataCompat> mediaList;
    private String selectedSubject;
    private MediaMetadataCompat selectedMedia;
    //private List<String> testingGson = new ArrayList<>();
    private List<MediaItem> cachedMediaItems;

    private FirebaseFirestore firestore;

    public static MediaBaseFragment newInstance(String subject) {
        MediaBaseFragment mediaBaseFragment = new MediaBaseFragment();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SELECTED_SUBJECT, subject);
        mediaBaseFragment.setArguments(bundle);
        return mediaBaseFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_media_base, container, false);
        firestore = FirebaseFirestore.getInstance();
        offlineTextView = view.findViewById(R.id.offline_text_view);


        cachedMediaItems = new ArrayList<>();
        if (getArguments() != null) {
            selectedSubject = getArguments().getString(KEY_SELECTED_SUBJECT);
            iMainActivity.getToolBar().setTitle(selectedSubject);
            Log.d(TAG, "onCreateView: selected subjects is :" + selectedSubject);
        }
        initRecyclerView();
        if (savedInstanceState != null) {
            adapter.setSelectedIndex(savedInstanceState.getInt("selected_index"));
        }

        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_index", adapter.getSelectedIndex());
    }


    private void initRecyclerView() {
        mediaList = new ArrayList<>();
        recyclerView = view.findViewById(R.id.media_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //TODO set the adapter after filling the list
        adapter = new MediaRecyclerAdapter(mediaList, getActivity());
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        if (mediaList.size() == 0 && isConnected()) {
            retrieveDate();
//            Log.d(TAG, "initRecyclerView: saved list is : "+ iMainActivity.getMyPreferenceManager().getOperativeRecords().get(0).getTitle());
//            Log.d(TAG, "initRecyclerView: saved list is : "+ iMainActivity.getMyPreferenceManager().getCrownRecords().get(0).getTitle());
        } else {
            if (iMainActivity.getMyPreferenceManager().getCrownRecords() == null) {
                offlineTextView.setVisibility(View.VISIBLE);
            } else {
//                Log.d(TAG, "initRecyclerView: saved list is : "+ iMainActivity.getMyPreferenceManager().getOperativeRecords().get(0).getTitle());
//                Log.d(TAG, "initRecyclerView: saved list is : "+ iMainActivity.getMyPreferenceManager().getCrownRecords().get(0).getTitle());
                if(!mediaList.isEmpty()){
                    mediaList.clear();
                }
                Toast.makeText(getActivity(), "retrieving cached data", Toast.LENGTH_SHORT).show();
                retrieveCachesData();
            }

        }
    }

    private void convertToCashedList(List<MediaMetadataCompat> mediaList) {
        int i = 0;
        cachedMediaItems.clear();
        for (MediaMetadataCompat mediaItem : mediaList) {
            cachedMediaItems.add(new MediaItem(
                    mediaItem.getDescription().getMediaId(),
                    mediaItem.getDescription().getTitle().toString(),
                    mediaItem.getDescription().getSubtitle().toString(),
                    mediaItem.getDescription().getMediaUri().toString()
            ));
            Log.d(TAG, "convertToCashedList: caches items is :" + cachedMediaItems.get(i).getId());
            Log.d(TAG, "convertToCashedList: caches items is :" + cachedMediaItems.get(i).getTitle());
            Log.d(TAG, "convertToCashedList: caches items is :" + cachedMediaItems.get(i).getDescription());
            Log.d(TAG, "convertToCashedList: caches items is :" + cachedMediaItems.get(i++).getMediaUrl());
        }
        cacheRecords();
        Toast.makeText(getActivity(), "media list saved", Toast.LENGTH_LONG).show();
        Log.d(TAG, "convertToCashedList: the cached list is :" + retrieveCachedRecords());
    }

    private void retrieveCachesData() {
        if(!mediaList.isEmpty()){
            mediaList.clear();
        }
        Log.d(TAG, "retrieveCachesData: selected subject is " + selectedSubject);
        Log.d(TAG, "retrieveCachesData: list is : " + retrieveCachedRecords().get(0).getTitle());
        for (MediaItem mediaItem : retrieveCachedRecords()) {
            MediaMetadataCompat mediaMetadataCompat = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaItem.getId())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mediaItem.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mediaItem.getDescription())
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaItem.getMediaUrl())
                    .build();

           mediaList.add(mediaMetadataCompat);
        }
    }

    private void cacheRecords() {
        Log.d(TAG, "cacheRecords: beeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeen HERE!!!");
        Toast.makeText(getActivity(), "Been here", Toast.LENGTH_LONG).show();
        switch (selectedSubject) {
            case "Crown":
                Toast.makeText(getActivity(), "Cached to Crown", Toast.LENGTH_SHORT).show();
                iMainActivity.getMyPreferenceManager().saveCrownRecords(this.cachedMediaItems);
                Log.d(TAG, "cacheRecords: records     " + iMainActivity.getMyPreferenceManager().getCrownRecords().get(0).getTitle());
                return;
            case "Operative":
                Toast.makeText(getActivity(), "Cached to operative", Toast.LENGTH_SHORT).show();
                iMainActivity.getMyPreferenceManager().saveOperativeRecords(this.cachedMediaItems);
                Log.d(TAG, "cacheRecords: records     " + iMainActivity.getMyPreferenceManager().getOperativeRecords().get(0).getTitle());
                return;
            case "Oral Pathology":
                iMainActivity.getMyPreferenceManager().saveOralRecords(this.cachedMediaItems);
                return;
            case "Ortho":
                iMainActivity.getMyPreferenceManager().saveOrthoRecords(this.cachedMediaItems);
                return;
            case "Pedo":
                iMainActivity.getMyPreferenceManager().savePedoRecords(this.cachedMediaItems);
                return;
            case "Pharma":
                iMainActivity.getMyPreferenceManager().savePharmaRecords(this.cachedMediaItems);
                return;
            case "Prosthesis":
                iMainActivity.getMyPreferenceManager().saveProsthesisRecords(this.cachedMediaItems);
                return;
        }
    }

    private List<MediaItem> retrieveCachedRecords() {
        Log.d(TAG, "retrieveCachedRecords: selected subject" + selectedSubject);
        switch (selectedSubject) {
            case "Crown":
                Log.d(TAG, "retrieveCachedRecords: " + iMainActivity.getMyPreferenceManager().getCrownRecords().get(0));
                return iMainActivity.getMyPreferenceManager().getCrownRecords();
            case "Operative":
                Log.d(TAG, "retrieveCachedRecords: " + iMainActivity.getMyPreferenceManager().getOperativeRecords().get(0));
                return iMainActivity.getMyPreferenceManager().getOperativeRecords();
            case "Oral Pathology":
                return iMainActivity.getMyPreferenceManager().getOralRecords();
            case "Ortho":
                return iMainActivity.getMyPreferenceManager().getOrthoRecords();
            case "Pedo":
                return iMainActivity.getMyPreferenceManager().getPedoRecords();
            case "Pharma":
                return iMainActivity.getMyPreferenceManager().getPharmaRecords();
            case "Prosthesis":
                return iMainActivity.getMyPreferenceManager().getProsthesisRecords();
            default:
                return iMainActivity.getMyPreferenceManager().getCrownRecords();
        }
    }

    private void retrieveDate() {
        iMainActivity.showProgress();
        Query query = firestore.collection("Subjects/")
                .document(selectedSubject)
                .collection("records/");

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        addToMediaList(document);
                    }
                    convertToCashedList(mediaList);
                } else {
                    Log.d(TAG, "onComplete: error getting documents: " + task.getException());
                }
                updateDataSet();
            }
        });
    }

    private void addToMediaList(QueryDocumentSnapshot document) {
        MediaMetadataCompat media = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, document.getId())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, document.get("title").toString())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, document.get("description").toString())
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, document.get("media_url").toString())
                .build();
        mediaList.add(media);
    }

    private boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private void getSelectedMediaItem(String mediaId) {
        for (MediaMetadataCompat media : mediaList) {
            if (media.getDescription().getMediaId().equals(mediaId)) {
                selectedMedia = media;
                adapter.setSelectedIndex(adapter.getIndexOfItem(selectedMedia));
                break;
            }
        }
    }

    private void updateDataSet() {
        iMainActivity.hideProgress();
        adapter.notifyDataSetChanged();

        if (iMainActivity.getMyPreferenceManager().getLastSubject().equals(selectedSubject)) {
            getSelectedMediaItem(iMainActivity.getMyPreferenceManager().getLastPlayedMedia());
        }
    }

    public void updateUI(MediaMetadataCompat mediaItem) {
        adapter.setSelectedIndex(adapter.getIndexOfItem(mediaItem));
        selectedMedia = mediaItem;
        saveLastPlayedSongProperties();
    }

    @Override
    public void onItemClick(int position) {
        adapter.setSelectedIndex(position);
        iMainActivity.getMyApplication().setMediaItems(mediaList);
        selectedMedia = mediaList.get(position);
        iMainActivity.onMediaSelected(selectedSubject, selectedMedia, position);
        saveLastPlayedSongProperties();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        iMainActivity = (MainActivity) getActivity();
    }

    private void saveLastPlayedSongProperties() {
        iMainActivity.getMyPreferenceManager().setPlaylistId(selectedSubject);
        iMainActivity.getMyPreferenceManager().saveLastPlayedMedia(selectedMedia.getDescription().getMediaId());
        iMainActivity.getMyPreferenceManager().saveLastPlayedSubject(selectedSubject);
        //TODO see if there is anything else to be saved in the shared preferences
    }
}
