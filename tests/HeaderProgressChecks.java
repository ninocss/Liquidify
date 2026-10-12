package dev.nino.squareglass;
public final class HeaderProgressChecks {
    static void check(boolean condition) { if(!condition)throw new AssertionError(); }
    public static void main(String[] args) {
        // A full down/up scroll must return to genuinely transparent, without retained state.
        for(float reach:new float[]{64,128,224}) {
            float previous=0;
            for(int y=0;y<=1000;y++) {
                float opacity=HeaderProgress.opacity(y,reach);
                check(opacity>=previous && opacity>=0 && opacity<=1);previous=opacity;
            }
            for(int y=1000;y>=0;y--) {
                float opacity=HeaderProgress.opacity(y,reach);
                check(opacity<=previous);previous=opacity;
            }
            check(previous==0);check(HeaderProgress.opacity(reach,reach)==1);
            check(HeaderProgress.opacity(reach/2,reach)==.5f);
            check(HeaderProgress.opacity(.01f,reach)<.001f);
        }
        check(HeaderProgress.opacity(-10,64)==0);
        check(HeaderProgress.opacity(10,0)==0);
        check(HeaderProgress.opacity(Float.NaN,64)==0);
        System.out.println("6006 scroll positions and invalid input checked");
    }
}
