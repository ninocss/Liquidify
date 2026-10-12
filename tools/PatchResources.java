/* Build-only adapter for the shared resource patcher. GPL-3.0-or-later. */
import dev.nino.squareglass.patching.ResourcePalettePatcher;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class PatchResources {
    static int hex(String s) { return (int)Long.parseLong(s,16); }
    public static void main(String[] args) throws Exception {
        var changes=new ArrayList<ResourcePalettePatcher.ColorChange>();
        for (String line:Files.readAllLines(Path.of(args[1]))) {
            if(line.isBlank() || line.startsWith("#"))continue;
            var c=line.split("\t");
            changes.add(new ResourcePalettePatcher.ColorChange(hex(c[0]),c[1],c[2],hex(c[3]),hex(c[4]),hex(c[5])));
        }
        byte[] input;
        try(var zip=new ZipFile(args[0])) { input=zip.getInputStream(zip.getEntry("resources.arsc")).readAllBytes(); }
        Files.write(Path.of(args[2]),ResourcePalettePatcher.patch(input,changes,Integer.parseInt(args[3])));
        System.out.println("Patched "+changes.size()+" semantic colors; preserved resource IDs");
    }
}
