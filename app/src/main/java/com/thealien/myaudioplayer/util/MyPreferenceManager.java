package com.thealien.myaudioplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thealien.myaudioplayer.models.MediaItem;

import java.lang.reflect.Type;
import java.util.List;

import static com.thealien.myaudioplayer.util.Constants.CHRONOMETER_FULL_DURATION;
import static com.thealien.myaudioplayer.util.Constants.KEY_SEEK_BAR_PROGRESS;
import static com.thealien.myaudioplayer.util.Constants.LAST_CATEGORY;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_CROWN;
import static com.thealien.myaudioplayer.util.Constants.MEDIA_QUEUE_POSITION;
import static com.thealien.myaudioplayer.util.Constants.NOW_PLAYING;
import static com.thealien.myaudioplayer.util.Constants.PLAYLIST_ID;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_OPERATIVE;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_ORAL;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_ORTHO;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_PEDO;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_PHARMA;
import static com.thealien.myaudioplayer.util.Constants.RECORDS_PROSTHESIS;
import static com.thealien.myaudioplayer.util.Constants.SPEED_INDEX;

public class MyPreferenceManager {

    private static final String TAG = "MyPreferenceManager";

    private SharedPreferences mPreferences;
    private static MyPreferenceManager myPreferenceManager;

    private MyPreferenceManager(Context context) {
        this.mPreferences = context.getSharedPreferences("prefs",Context.MODE_PRIVATE);
    }

    public static MyPreferenceManager getInstance(Context context){
        if(myPreferenceManager == null){
            myPreferenceManager = new MyPreferenceManager(context);
        }
        return myPreferenceManager;
    }

    public void setPlaybackSpeedIndex(int index){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(SPEED_INDEX,index);
        editor.apply();
    }

    public int getSpeedIndex(){
        return mPreferences.getInt(SPEED_INDEX,1);
    }
    public void setPlaylistId(String playlistId){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PLAYLIST_ID,playlistId);
        editor.apply();
    }
    public String getPlaylistId(){
        return mPreferences.getString(PLAYLIST_ID,"");
    }

    public void saveQueuePosition(int position){
        Log.d(TAG, "saveQueuePosition: SAVING QUEUE INDEX: " + position);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(MEDIA_QUEUE_POSITION, position);
        editor.apply();
    }

    public void saveSeekBarPosition(int position){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(KEY_SEEK_BAR_PROGRESS,position);
        editor.apply();
    }

    public void saveChronometerDuration(long duration){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putLong(CHRONOMETER_FULL_DURATION,duration);
        editor.apply();
    }

    public long getChronometerDuration(){
        return mPreferences.getLong(CHRONOMETER_FULL_DURATION,0);
    }

    public int getSeekBarPosition(){
        return mPreferences.getInt(KEY_SEEK_BAR_PROGRESS,0);
    }

    public int getQueuePosition(){
        return mPreferences.getInt(MEDIA_QUEUE_POSITION, -1);
    }


    public String getLastSubject(){
        return  mPreferences.getString(LAST_CATEGORY, "");
    }

    public void saveLastPlayedMedia(String mediaId){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(NOW_PLAYING, mediaId);
        editor.apply();
    }

    public String getLastPlayedMedia(){
        return mPreferences.getString(NOW_PLAYING, "");
    }

    public void saveLastPlayedSubject(String category){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(LAST_CATEGORY, category);
        editor.apply();
    }

    public void saveCrownRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_CROWN,json);
        editor.apply();

    }

    public List<MediaItem> getCrownRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_CROWN,"");
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void saveOperativeRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_OPERATIVE,json);
        editor.apply();
    }

    public List<MediaItem> getOperativeRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_OPERATIVE,"");
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void saveOralRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_ORAL,json);
        editor.apply();
    }

    public List<MediaItem> getOralRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_ORAL,null);
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void saveOrthoRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_ORTHO,json);
        editor.apply();
    }

    public List<MediaItem> getOrthoRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_ORTHO,null);
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void savePedoRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_PEDO,json);
        editor.apply();
    }

    public List<MediaItem> getPedoRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_PEDO,null);
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void savePharmaRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_PHARMA,json);
        editor.apply();
    }

    public List<MediaItem> getPharmaRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_PHARMA,null);
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void saveProsthesisRecords(List<MediaItem> list){
        SharedPreferences.Editor editor = mPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(RECORDS_PROSTHESIS,json);
        editor.apply();
    }

    public List<MediaItem> getProsthesisRecords(){
        Gson gson = new Gson();
        String json = mPreferences.getString(RECORDS_PROSTHESIS,null);
        Type type = new TypeToken<List<MediaItem>>(){}.getType();
        return gson.fromJson(json,type);
    }

    public void saveFirstLaunch(boolean firsTimeLaunch){
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean("first_time",firsTimeLaunch);
        editor.apply();
    }

    public boolean isFirstLaunch(){
        return mPreferences.getBoolean("first_time",false);
    }
}
