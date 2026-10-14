package dev.nino.squareglass;

public final class BackdropCaptureChecks {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        BackdropCapturePolicy policy = new BackdropCapturePolicy();
        check(!policy.allowCapture(false));
        check(!policy.allowCapture(false));
        check(policy.allowCapture(false));
        policy.transition();
        // An exit may remain active after its View leaves normal childCount.
        for (int i=0; i<120; i++) check(!policy.allowCapture(true));
        check(!policy.allowCapture(false));
        check(!policy.allowCapture(false));
        check(policy.allowCapture(false));
        // A replacement/back transaction during settling restarts the guard.
        policy.transition();
        check(!policy.allowCapture(false));
        policy.transition();
        check(!policy.allowCapture(false));
        check(!policy.allowCapture(false));
        check(policy.allowCapture(false));
        check(policy.allowCapture(false));
        System.out.println("Backdrop capture: active exits and overlapping transactions stay native; settled capture resumes");
    }
}
