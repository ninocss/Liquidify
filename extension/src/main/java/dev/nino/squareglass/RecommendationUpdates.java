/* GPL-3.0-or-later. Coalesce only native recommendation Add operations. */
package dev.nino.squareglass;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.recyclerview.widget.*;
import java.lang.ref.WeakReference;
import java.util.*;

public final class RecommendationUpdates {
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static final WeakHashMap<Object,Pending> pending=new WeakHashMap<>();
    private static final WeakHashMap<Object,Revision> recent=new WeakHashMap<>();
    private static final class Revision {
        final String value;final WeakReference<RecyclerView> view;final long time=android.os.SystemClock.elapsedRealtime();
        Revision(String value,RecyclerView view) {this.value=value;this.view=new WeakReference<>(view);}
    }
    private RecommendationUpdates() {}
    public static void begin(Object presenter) {
        if(SquareSettings.themeEnabled()==0 || pending.containsKey(presenter))return;
        View view=NativePlaylistBridge.view(presenter);
        if(!(view instanceof RecyclerView))return;
        Pending state=new Pending(presenter,(RecyclerView)view,NativePlaylistBridge.items(presenter));
        pending.put(presenter,state);
        View click=PageCoordinator.lastClick();
        if(click!=null && click.isEnabled() && inside(click,view)) {
            state.button=new WeakReference<>(click);click.setEnabled(false);
        }
        ((View)view).addOnAttachStateChangeListener(state);
        main.postDelayed(state,10000); // Error/abandoned subscription must never leave a frozen adapter.
    }
    private static boolean inside(View child,View root) {
        for(View current=child;current!=null;) {
            if(current==root)return true;
            current=current.getParent() instanceof View?(View)current.getParent():null;
        }
        return false;
    }
    public static boolean active(Object presenter) {return pending.containsKey(presenter);}
    public static void added(Object presenter,Object playlist) {
        Pending state=pending.get(presenter);if(state!=null)state.revision=NativePlaylistBridge.revision(playlist);
    }
    public static void manual(Object presenter) {recent.remove(presenter);}
    public static boolean reuse(Object presenter,Object playlist) {
        Pending state=pending.get(presenter);Revision done=recent.get(presenter);
        boolean duplicate=done!=null && android.os.SystemClock.elapsedRealtime()-done.time<15000 &&
            done.view.get()==NativePlaylistBridge.view(presenter) && done.value.equals(NativePlaylistBridge.revision(playlist));
        if(state==null && !duplicate) {recent.remove(presenter);return false;}
        NativePlaylistBridge.metadata(presenter,playlist);
        if(state!=null)preserve(presenter);
        if(ProbeDiagnostics.enabled)Log.d("SquareGlass","Recommendation duplicate rebind retained");
        return true;
    }
    public static boolean handle(Object presenter,Object model) {
        Pending state=pending.get(presenter);
        if(state==null)return false;
        if(ProbeDiagnostics.enabled)Log.d("SquareGlass","Recommendation model coalesced");
        NativePlaylistBridge.model(presenter,model);state.changed=true;
        if(state.completed)state.schedule(100);
        return true;
    }
    /** Called by the matching native playlist event even when it would not scroll. */
    public static boolean preserve(Object presenter) {
        Pending state=pending.get(presenter);if(state==null)return false;
        if(ProbeDiagnostics.enabled)Log.d("SquareGlass","Recommendation matching event/reload retained");
        state.event=true;
        if(state.completed)state.schedule(100);
        return true;
    }
    public static void completed(Object presenter,boolean success) {
        Pending state=pending.get(presenter);if(state==null)return;
        state.completed=true;state.success=success;state.schedule(!success || state.event?100:750);
    }
    private static final class Pending implements Runnable,View.OnAttachStateChangeListener {
        final WeakReference<Object> owner;final WeakReference<RecyclerView> view;
        final List<?> before;WeakReference<View> button=new WeakReference<>(null);
        boolean event,completed,changed,success;String revision;
        Pending(Object owner,RecyclerView view,List<?> before) {
            this.owner=new WeakReference<>(owner);this.view=new WeakReference<>(view);
            this.before=new ArrayList<>(before);
        }
        void schedule(long delay) {main.removeCallbacks(this);main.postDelayed(this,delay);}
        @Override public void run() {
            Object presenter=owner.get();RecyclerView list=view.get();
            main.removeCallbacks(this);if(presenter!=null)pending.remove(presenter);
            View control=button.get();if(control!=null)control.setEnabled(true);
            if(list!=null)((View)list).removeOnAttachStateChangeListener(this);
            if(presenter==null || list==null || !changed)return;
            if(success)recent.put(presenter,new Revision(NativePlaylistBridge.contentRevision(presenter),list));
            // Capture at dispatch, so scrolling while the network request runs remains respected.
            RecyclerView.LayoutManager layout=list.getLayoutManager();
            LinearLayoutManager linear=layout instanceof LinearLayoutManager?(LinearLayoutManager)layout:null;
            int position=linear==null?-1:linear.findFirstVisibleItemPosition();
            View anchor=position<0?null:linear.findViewByPosition(position);
            // A collapsing AppBar may scroll while the RecyclerView still reports row zero.
            // Preserve the interacted recommendation, using its current coordinates at dispatch.
            View clicked=control;
            while(clicked!=null && clicked.getParent()!=list)
                clicked=clicked.getParent() instanceof View?(View)clicked.getParent():null;
            if(clicked!=null && list.getChildAdapterPosition(clicked)>=0) {
                anchor=clicked;position=list.getChildAdapterPosition(clicked);
            }
            int offset=anchor==null?0:anchor.getTop()-((View)list).getPaddingTop();
            int[] location=new int[2];if(anchor!=null)anchor.getLocationInWindow(location);
            final int screenTop=location[1];
            DiffUtil.Callback callback=(DiffUtil.Callback)NativePlaylistBridge.callback(presenter,before);
            DiffUtil.DiffResult diff=DiffUtil.calculateDiff(callback);
            int mapped=-1;
            // When the visible recommendation was removed, retain its nearest surviving neighbour.
            if(position>=0 && position<before.size()) {
                for(int distance=0;distance<before.size() && mapped<0;distance++) {
                    int old=position+distance;
                    if(old<before.size())mapped=diff.convertOldPositionToNew(old);
                    if(mapped<0 && distance>0 && position-distance>=0)mapped=diff.convertOldPositionToNew(position-distance);
                }
            }
            RecyclerView.ItemAnimator animator=list.getItemAnimator();
            list.setItemAnimator(null);
            NativePlaylistBridge.dispatch(presenter,diff);
            if(linear!=null && mapped>=0) {
                final int target=mapped;
                ((View)list).getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        if(((View)list).getViewTreeObserver().isAlive())((View)list).getViewTreeObserver().removeOnPreDrawListener(this);
                        View row=linear.findViewByPosition(target);
                        if(row!=null) {
                            int[] now=new int[2];row.getLocationInWindow(now);int delta=now[1]-screenTop;
                            int[] consumed=new int[2];
                            list.startNestedScroll(View.SCROLL_AXIS_VERTICAL,androidx.core.view.ViewCompat.TYPE_NON_TOUCH);
                            list.dispatchNestedPreScroll(0,delta,consumed,null,androidx.core.view.ViewCompat.TYPE_NON_TOUCH);
                            list.scrollBy(0,delta-consumed[1]);list.stopNestedScroll(androidx.core.view.ViewCompat.TYPE_NON_TOUCH);
                        }
                        list.setItemAnimator(animator);
                        return true;
                    }
                });
            } else list.setItemAnimator(animator);
            Log.d("SquareGlass","Recommendation Add: one native diff; anchor="+position+" -> "+mapped+" offset="+offset);
        }
        @Override public void onViewAttachedToWindow(View v) {}
        @Override public void onViewDetachedFromWindow(View v) {run();}
    }
}
