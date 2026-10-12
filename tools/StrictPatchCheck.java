/* Uses the Manager's actual parser and strict patch engine. GPL-3.0-or-later. */
import java.nio.file.*;
import java.util.*;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;

public class StrictPatchCheck {
    public static void main(String[] args) throws Exception {
        Path originals=Path.of(args[0]), patches=Path.of(args[1]), expected=Path.of(args[2]);
        int checked=0;
        var outputs=new HashMap<String,List<String>>();
        for (Path patch : Files.list(patches).filter(p->p.toString().endsWith(".patch")).sorted().toList()) {
            var lines=Files.readAllLines(patch);
            String target=lines.stream().filter(s->s.startsWith("--- a/")).findFirst().orElseThrow().substring(6);
            if (target.contains("..")) throw new IllegalArgumentException("Unsafe target");
            var source=outputs.getOrDefault(target,Files.readAllLines(originals.resolve(target)));
            var result=DiffUtils.patch(source, UnifiedDiffUtils.parseUnifiedDiff(lines));
            outputs.put(target,result);
            checked++;
        }
        for (var entry:outputs.entrySet()) if (!entry.getValue().equals(Files.readAllLines(expected.resolve(entry.getKey()))))
            throw new IllegalStateException("Unexpected output for "+entry.getKey());
        System.out.println("Strict Manager diff engine verified "+checked+" patches against baksmali output");
    }
}
