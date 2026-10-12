"""Compile-only pinned host ABI for the Linn provider; none is shipped."""
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'host-stubs/src/main/java'

def write(name, body):
    path=DEST/(name.replace('.','/')+'.java');path.parent.mkdir(parents=True,exist_ok=True)
    package,_,simple=name.rpartition('.')
    path.write_text('package '+package+';\n'+body+'\n',encoding='utf-8')

def main():
    write('p2.a','public abstract class a { public final String a,b; public final int c; public a(String id,String name,int type){a=id;b=name;c=type;} public boolean a(){return false;} }')
    write('com.aspiro.wamp.broadcast.o','public interface o { interface a {} void addListener(l listener); void connect(p2.a device); void disconnect(a callback); io.reactivex.Single<java.util.List<p2.a>> getAllAvailableDevices(); p getBroadcastProviderButton(); r getBroadcastProviderGroupButton(); p2.a getConnectedItem(); fq.f getVolumeControl(); void init(); boolean isConnected(); boolean isConnecting(); boolean isValidItem(p2.a device); void startScanning(); void stopScanning(); }')
    write('com.aspiro.wamp.broadcast.q','public interface q { interface a {} void addListener(a listener); o get(); boolean isAvailable(); void refreshAvailability(); }')
    write('com.aspiro.wamp.broadcast.l','public interface l { void a(o provider); void b(o provider,int error); void c(); void d(o provider); }')
    for name in ['com.aspiro.wamp.broadcast.p','com.aspiro.wamp.broadcast.r','li.a']:
        write(name,'public interface '+name.rsplit('.',1)[-1]+' {}')
    write('fq.f','public interface f { rx.subjects.f getMaxVolumeSubject(); rx.subjects.f getUpdateVolumeSubject(); void startListening(); void stopListening(); void updateVolume(int value); }')
    write('rx.s','public interface s { void onNext(Object value); }')
    write('rx.subjects.f','public abstract class f implements rx.s {}')
    write('rx.subjects.PublishSubject','public class PublishSubject extends f { public static PublishSubject a(){return null;} public void onNext(Object value){} }')
    write('com.aspiro.wamp.player.PlaybackEndReason','public enum PlaybackEndReason { UNKNOWN }')
    text=(ROOT/'research/tidal/smali/com/aspiro/wamp/player/k0.smali').read_text()
    def typename(t):
        return {'V':'void','I':'int','Z':'boolean'}.get(t,t[1:-1].replace('/','.').replace('$','.'))
    methods=[]
    for name,args,result in re.findall(r'^\.method public (?:abstract )?(\w+)\(([^)]*)\)(\S+)',text,re.M):
        types=re.findall(r'L[^;]+;|[IZ]',args)
        methods.append(typename(result)+' '+name+'('+','.join(typename(t)+' arg'+str(i) for i,t in enumerate(types))+');')
    write('com.aspiro.wamp.player.k0','public interface k0 { '+' '.join(methods)+' }')

if __name__=='__main__':main()
