package androidx.media;
public abstract class VolumeProviderCompat {
    public static final int VOLUME_CONTROL_ABSOLUTE=2;
    public VolumeProviderCompat(int control,int max,int current){}
    public void setCurrentVolume(int value){}
    public int getCurrentVolume(){return 0;}
    public void onSetVolumeTo(int value){}
    public void onAdjustVolume(int direction){}
}
