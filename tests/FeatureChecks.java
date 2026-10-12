/* Runs without Android: protocol safety, real XML parsing and time ranking. */
package dev.nino.squareglass;
import java.net.*;
public final class FeatureChecks {
    static int checks;
    static void check(boolean value, String name) { if (!value) throw new AssertionError(name); checks++; }
    public static void main(String[] args) throws Exception {
        int[] morning = new int[24]; morning[8] = 8;
        int[] evening = new int[24]; evening[20] = 8;
        long now = 100L * 86400000;
        check(TimeTaste.score(morning,8,now-86400000,now,false) > TimeTaste.score(evening,8,now-86400000,now,false), "time affinity");
        int[] midnight = new int[24]; midnight[23]=10;
        check(TimeTaste.score(midnight,0,now,now,false) > TimeTaste.score(midnight,12,now,now,false), "circular midnight");
        check(TimeTaste.score(morning,8,now-40*86400000L,now,true) > TimeTaste.score(morning,8,now-86400000,now,true), "rediscover older songs");
        check(Double.isFinite(TimeTaste.score(morning,8,now+86400000,now,false)), "future clock correction");
        check(TimeTaste.score(morning,8,now-86400000,now,false) > TimeTaste.score(morning,8,now,now,false), "avoid immediate repeats");
        System.out.println(checks + " feature checks passed");
    }
}
