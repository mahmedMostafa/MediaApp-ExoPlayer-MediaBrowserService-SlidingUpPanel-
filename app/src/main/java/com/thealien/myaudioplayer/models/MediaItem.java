package com.thealien.myaudioplayer.models;


//this class is gonna be used for caching to work with gson properly
public class MediaItem {

    private String id;
    private String title;
    private String description;
    private String mediaUrl;

    public MediaItem(String id, String title, String description, String mediaUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.mediaUrl = mediaUrl;
    }

    public MediaItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
