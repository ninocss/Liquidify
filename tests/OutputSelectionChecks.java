package dev.nino.squareglass;

public final class OutputSelectionChecks {
    private static void check(boolean value) {if(!value)throw new AssertionError();}
    public static void main(String[] args) {
        OutputSelection state=new OutputSelection();
        int old=state.nextRequest(),current=state.nextRequest();
        check(!state.accepts(old));check(state.accepts(current));
        state.begin("uuid:new",100);
        check(state.observe("uuid:old",200)==0);check("uuid:new".equals(state.target()));
        check(state.observe("uuid:new",300)==1);check(state.target()==null);
        state.begin("uuid:unreachable",1000);
        check(state.observe("uuid:old",15999)==0);
        check(state.observe("uuid:old",16000)==-1);check(state.target()==null);
        state.begin("uuid:retry",20000);state.failed();check(state.target()==null);
        state.invalidate();check(!state.accepts(current));
        // A disconnected session never confirms a pending transfer.
        state.begin("uuid:device",0);check(state.observe(null,100)==0);
    }
}
