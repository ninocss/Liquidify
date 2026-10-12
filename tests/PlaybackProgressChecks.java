package dev.nino.squareglass;
public final class PlaybackProgressChecks {
    public static void main(String[] args) {
        check(PlaybackProgress.fraction(0,0)==-1f,"unavailable duration");
        check(PlaybackProgress.fraction(900,-1)==-1f,"native unknown duration");
        check(PlaybackProgress.fraction(-20,1000)==0f,"negative position");
        check(PlaybackProgress.fraction(3000,1000)==1f,"queue switch overshoot");
        check(PlaybackProgress.fraction(500,1000)==.5f,"native milliseconds");
        check(PlaybackProgress.fraction(Integer.MAX_VALUE,Integer.MAX_VALUE)==1f,"no integer overflow");
    }
    static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
