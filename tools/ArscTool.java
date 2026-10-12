/* Build-time resource inspection. Uses the Manager's pinned parser. */
import com.google.devrel.gmscore.tools.apk.arsc.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class ArscTool {
    public static void main(String[] args) throws Exception {
        byte[] bytes;
        try (var zip = new ZipFile(args[0])) { bytes = zip.getInputStream(zip.getEntry("resources.arsc")).readAllBytes(); }
        var arsc = new BinaryResourceFile(bytes);
        var table = (ResourceTableChunk)arsc.getChunks().get(0);
        for (var pkg : table.getPackages()) {
            for (var chunk : pkg.getTypeChunks()) {
                if (!chunk.getTypeName().equals("color")) continue;
                for (int i = 0; i < chunk.getTotalEntryCount(); i++) {
                    var entry = chunk.getEntry(i);
                    if (entry == null || entry.isComplex()) continue;
                    System.out.printf("%08x\t%s\t%s\t%s\t%08x%n", (pkg.getId()<<24)|(chunk.getId()<<16)|i,
                        chunk.getConfiguration(), entry.key(), entry.value().type(), entry.value().data());
                }
            }
        }
    }
}
