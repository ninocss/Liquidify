/* Integration checks against the pinned APK's real resource table. */
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import com.google.devrel.gmscore.tools.apk.arsc.*;
import dev.nino.squareglass.patching.ResourcePalettePatcher;
import dev.nino.squareglass.patching.ResourcePalettePatcher.ColorChange;

public class ResourcePaletteCheck {
    static int hex(String s) { return (int)Long.parseLong(s,16); }
    static Map<String,TypeChunk> colors(byte[] data) {
        var table=(ResourceTableChunk)new BinaryResourceFile(data).getChunks().get(0);
        var result=new HashMap<String,TypeChunk>();
        for (var pkg:table.getPackages()) for (var t:pkg.getTypeChunks())
            if (t.getTypeName().equals("color")) result.put(t.getConfiguration().toString(),t);
        return result;
    }
    static void reject(byte[] original,List<ColorChange> changes,int mode) {
        try { ResourcePalettePatcher.patch(original,changes,mode); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid resource request was accepted");
    }
    public static void main(String[] args) throws Exception {
        byte[] original;
        try (var zip=new ZipFile(args[0])) { original=zip.getInputStream(zip.getEntry("resources.arsc")).readAllBytes(); }
        var changes=new ArrayList<ColorChange>();
        for (var line:Files.readAllLines(Path.of(args[1]))) {
            if (line.isBlank() || line.startsWith("#")) continue;
            var p=line.split("\t");
            changes.add(new ColorChange(hex(p[0]),p[1],p[2],hex(p[3]),hex(p[4]),hex(p[5])));
        }
        var before=colors(original);
        var ids=new HashSet<Integer>();
        changes.forEach(c->ids.add(c.id&65535));
        for (int mode=0;mode<=2;mode++) {
            var after=colors(ResourcePalettePatcher.patch(original,changes,mode));
            var defaults=after.values().stream().filter(t->t.getConfiguration().isDefault()).findFirst().orElseThrow();
            var night=after.get("night");
            if (night==null) throw new AssertionError("Night colors missing");
            for (var c:changes) {
                var lightEntry=defaults.getEntry(c.id&65535);
                var darkEntry=night.getEntry(c.id&65535);
                if (!lightEntry.key().equals(c.name) || !darkEntry.key().equals(c.name) ||
                    lightEntry.value().data()!=(mode==2?c.dark:c.light) ||
                    darkEntry.value().data()!=(mode==1?c.light:c.dark)) throw new AssertionError("Wrong color/configuration: "+c.name+" mode="+mode+" default="+Integer.toHexString(lightEntry.value().data())+" night="+Integer.toHexString(darkEntry.value().data())+" keys="+lightEntry.key()+"/"+darkEntry.key());
            }
            for (var entry:before.entrySet()) {
                var old=entry.getValue(); var now=after.get(entry.getKey());
                if (now==null || old.getId()!=now.getId() || old.getTotalEntryCount()!=now.getTotalEntryCount())
                    throw new AssertionError("Color type identity changed");
                for (int i=0;i<old.getTotalEntryCount();i++) {
                    var a=old.getEntry(i); var b=now.getEntry(i);
                    if (ids.contains(i) && (old.getConfiguration().isDefault() || entry.getKey().equals("night"))) continue;
                    if ((a==null)!=(b==null)) throw new AssertionError("Unrelated entry presence changed");
                    if (a!=null && (!a.key().equals(b.key()) || a.isComplex()!=b.isComplex() ||
                        (!a.isComplex() && (a.value().data()!=b.value().data() || a.value().type()!=b.value().type()))))
                        throw new AssertionError("Unrelated color changed");
                }
            }
        }
        var first=changes.get(0);
        var bad=new ArrayList<>(changes);
        bad.set(0,new ColorChange(first.id,first.name,first.expectedType,first.expectedData^1,first.light,first.dark));
        reject(original,bad,0);
        bad=new ArrayList<>(changes); bad.add(first); reject(original,bad,0);
        reject(original,changes,3);
        System.out.println("Verified "+changes.size()+" resources in system/light/dark; unrelated colors and IDs retained; invalid requests rejected");
    }
}
