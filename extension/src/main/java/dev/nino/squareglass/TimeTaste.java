/* GPL-3.0-or-later. Local, deterministic listening-time ranking. */
package dev.nino.squareglass;

public final class TimeTaste {
    private TimeTaste() {}
    public static double score(int[] hours, int hour, long lastPlayed, long now, boolean rediscover) {
        double affinity = 0;
        for (int i = 0; i < 24; i++) {
            int distance = Math.abs(i - hour); distance = Math.min(distance, 24 - distance);
            affinity += Math.log1p(Math.max(0, hours[i])) * Math.exp(-distance * distance / 8.0);
        }
        double days = Math.max(0, now - lastPlayed) / 86400000.0;
        return rediscover ? Math.log1p(days) + affinity * .15 :
            affinity * (0.7 + 0.3 * Math.exp(-days / 60)) * (days < .02 ? .25 : 1);
    }
}
