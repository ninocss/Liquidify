/* GPL-3.0-or-later. Shared artwork palette, independent of Android rendering. */
package dev.nino.squareglass;

public final class CoverColors {
    private CoverColors() {}
    public static int blend(int from, int to, float amount) {
        int r = Math.round(((from >>> 16) & 255) * (1 - amount) + ((to >>> 16) & 255) * amount);
        int g = Math.round(((from >>> 8) & 255) * (1 - amount) + ((to >>> 8) & 255) * amount);
        int b = Math.round((from & 255) * (1 - amount) + (to & 255) * amount);
        return 0xff000000 | r << 16 | g << 8 | b;
    }
    public static int surface(int seed, boolean dark) {
        int base = dark ? 0xff0d0e11 : 0xfff1f2f6;
        int secondary = dark ? 0xffb4b8c4 : 0xff5d626d;
        float film = dark ? .52f : .35f;
        int result = blend(seed, base, film);
        // Keep the host's secondary text readable, even for white/neon artwork.
        while (contrast(result, secondary) < 4.5 && film < 1f) {
            film = Math.min(1f, film + .02f);
            result = blend(seed, base, film);
        }
        return result;
    }
    public static int playerSurface(int seed, boolean dark) {
        if (!dark) return surface(seed, false);
        float film=.58f;
        int result=blend(seed,0xff242426,film);
        while (contrast(result,0xffffffff)<5.5 && film<1f) {
            film=Math.min(1f,film+.02f); result=blend(seed,0xff242426,film);
        }
        return result;
    }
    public static int seed(int[] pixels) {
        double[] red=new double[13],green=new double[13],blue=new double[13],weights=new double[13];
        for (int color : pixels) {
            if ((color >>> 24) < 128) continue;
            int r=(color >>> 16)&255, g=(color >>> 8)&255, b=color&255;
            int high=Math.max(r,Math.max(g,b)), low=Math.min(r,Math.min(g,b));
            int chroma=high-low, bucket=12;
            if (chroma>=24) {
                double hue=high==r ? (g-b)/(double)chroma : high==g ? 2+(b-r)/(double)chroma : 4+(r-g)/(double)chroma;
                if (hue<0) hue+=6;
                bucket=Math.min(11,(int)(hue*2));
            }
            double weight=bucket==12 ? .25 : 1+chroma/128.0;
            red[bucket]+=r*weight; green[bucket]+=g*weight; blue[bucket]+=b*weight; weights[bucket]+=weight;
        }
        int dominant=0;
        for (int i=1;i<13;i++) if (weights[i]>weights[dominant]) dominant=i;
        double total=weights[dominant];
        return total == 0 ? 0xff808080 : 0xff000000 | (int)Math.round(red[dominant]/total)<<16 |
                (int)Math.round(green[dominant]/total)<<8 | (int)Math.round(blue[dominant]/total);
    }
    private static double channel(int value) {
        double v=value/255.0;
        return v<=.04045 ? v/12.92 : Math.pow((v+.055)/1.055,2.4);
    }
    private static double luminance(int color) {
        return .2126*channel((color>>>16)&255)+.7152*channel((color>>>8)&255)+.0722*channel(color&255);
    }
    public static double contrast(int first,int second) {
        double a=luminance(first),b=luminance(second);
        return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    }
}
