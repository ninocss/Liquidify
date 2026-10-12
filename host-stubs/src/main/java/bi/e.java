package bi;
public interface e {
    com.aspiro.wamp.playqueue.u0 getCurrentItem();
    com.aspiro.wamp.playqueue.y getPlayQueue();
    int getCurrentMediaPosition();
    int getCurrentMediaDuration();
    boolean hasNext();
    boolean canSkipToPreviousOrRewind();
    boolean isLocal();
    void j();
    void h(boolean rewind);
    void d();
    void b(int index, boolean keep, boolean play);
}
