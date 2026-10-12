package com.aspiro.wamp.playqueue;
public interface y {
    u0 getCurrentItem();
    boolean isShuffled();
    com.aspiro.wamp.enums.RepeatMode getRepeatMode();
    java.util.List<u0> getItems();
    int getCurrentItemPosition();
    u0 goTo(int index, boolean update);
    void prepare(com.aspiro.wamp.playqueue.source.model.Source source, w0 options);
}
