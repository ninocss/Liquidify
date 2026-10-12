package com.aspiro.wamp.model;
public abstract class MediaItem implements java.io.Serializable {
    public void setId(int id){}
    public void setTitle(String title){}
    public void setDuration(int seconds){}
    public void setArtist(Artist artist){}
    public void setArtists(java.util.List<Artist> artists){}
    public void setAlbum(Album album){}
    public void setAllowStreaming(boolean value){}
    public void setStreamReady(boolean value){}
    public int getId() { return 0; }
    public abstract String getDisplayTitle();
    public String getArtistNames() { return null; }
    public Album getAlbum() { return null; }
    public int getDuration() { return 0; }
    public void setSource(com.aspiro.wamp.playqueue.source.model.Source source) {}
}
