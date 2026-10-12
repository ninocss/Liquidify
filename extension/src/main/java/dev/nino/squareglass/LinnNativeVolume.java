package dev.nino.squareglass;

/** Pinned native broadcast-slider model, in addition to Android media-session volume. */
final class LinnNativeVolume implements fq.f {
    final LinnProvider provider;
    final rx.subjects.PublishSubject maximum=rx.subjects.PublishSubject.a();
    final rx.subjects.PublishSubject updates=rx.subjects.PublishSubject.a();
    boolean listening;
    LinnNativeVolume(LinnProvider provider){this.provider=provider;}
    public rx.subjects.f getMaxVolumeSubject(){return maximum;}
    public rx.subjects.f getUpdateVolumeSubject(){return updates;}
    public void startListening(){listening=true;provider.main.post(() -> {
        if(!listening)return;maximum.onNext(100);
        LinnPlayer player=provider.player;if(player!=null && player.volume!=null)updates.onNext(player.volume.getCurrentVolume());
    });}
    public void stopListening(){listening=false;}
    public void updateVolume(int value){LinnPlayer player=provider.player;if(player!=null && player.volume!=null)player.volume.onSetVolumeTo(value);}
    void changed(int value){if(listening)updates.onNext(value);}
}
