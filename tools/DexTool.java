/* Build-time inspection only. Uses the same dexlib/smali version as the Manager. */
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.baksmali.*;
import com.android.tools.smali.smali.*;

public class DexTool {
    static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
    static String method(MethodReference m) {
        StringBuilder b = new StringBuilder(m.getName()).append('(');
        for (CharSequence p : m.getParameterTypes()) b.append(p);
        return b.append(')').append(m.getReturnType()).toString();
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("index|refs|disassemble|assemble INPUT OUTPUT");
        if (args[0].equals("assemble")) {
            SmaliOptions opts = new SmaliOptions();
            opts.outputDexFile = args[2];
            opts.apiLevel = 33;
            opts.jobs = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            if (!Smali.assemble(opts, List.of(args[1]))) throw new IOException("Smali assembly failed");
            return;
        }
        var container = DexFileFactory.loadDexContainer(new File(args[1]), Opcodes.getDefault());
        if (args[0].equals("disassemble") || args[0].equals("disassemble-targets")) {
            var opts = new BaksmaliOptions();
            opts.localsDirective = true;
            opts.sequentialLabels = true;
            for (String name : container.getDexEntryNames()) {
                var dex = container.getEntry(name).getDexFile();
                boolean success = args[0].equals("disassemble-targets")
                    ? Baksmali.disassembleDexFile(dex, new File(args[2]), 4, opts, Files.readAllLines(Path.of(args[3])))
                    : Baksmali.disassembleDexFile(dex, new File(args[2]), 4, opts);
                if (!success) throw new IOException("Disassembly failed");
            }
            return;
        }
        try (var out = Files.newBufferedWriter(Path.of(args[2]), StandardCharsets.UTF_8)) {
            if (args[0].equals("refs")) {
                Set<String> types = new TreeSet<>(), methods = new TreeSet<>(), fields = new TreeSet<>();
                for (String name : container.getDexEntryNames()) {
                    DexBackedDexFile dex = container.getEntry(name).getDexFile();
                    for (TypeReference t : dex.getTypeReferences()) types.add(t.getType());
                    for (MethodReference m : dex.getMethodSection()) methods.add(m.getDefiningClass() + "->" + method(m));
                    for (FieldReference f : dex.getFieldSection()) fields.add(f.getDefiningClass() + "->" + f.getName() + ":" + f.getType());
                }
                out.write("{\"types\":" + array(types) + ",\"methods\":" + array(methods) + ",\"fields\":" + array(fields) + "}");
            } else if (args[0].equals("index")) {
                out.write("{"); boolean first = true;
                for (String name : container.getDexEntryNames()) {
                    for (ClassDef c : container.getEntry(name).getDexFile().getClasses()) {
                        if (!first) out.write(",\n"); first = false;
                        Set<String> methods = new TreeSet<>(), fields = new TreeSet<>();
                        for (Method m : c.getMethods()) methods.add(method(m));
                        for (Field f : c.getFields()) fields.add(f.getName() + ":" + f.getType());
                        out.write(quote(c.getType()) + ":{\"super\":" + (c.getSuperclass() == null ? "null" : quote(c.getSuperclass()))
                            + ",\"interfaces\":" + array(c.getInterfaces()) + ",\"methods\":" + array(methods) + ",\"fields\":" + array(fields) + "}");
                    }
                }
                out.write("}");
            } else throw new IllegalArgumentException("Unknown command " + args[0]);
        }
    }
    static String array(Collection<? extends CharSequence> entries) {
        StringJoiner result = new StringJoiner(",", "[", "]");
        for (CharSequence s : entries) result.add(quote(s.toString()));
        return result.toString();
    }
}
