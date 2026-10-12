/* Local side-by-side test build. Never changes TIDAL class names or exports internal activities. */
import java.nio.file.*;
import java.util.Set;
import pxb.android.axml.*;

public class ManifestTest {
    static final String ANDROID = "http://schemas.android.com/apk/res/android";
    static final String ORIGINAL = "com.aspiro.tidal";
    static final String TEST = "dev.nino.tidalglass.test";
    static final Set<String> COMPONENTS = Set.of("application", "activity", "activity-alias", "service", "receiver", "provider");

    static NodeVisitor visit(NodeVisitor delegate, String element) {
        return new NodeVisitor(delegate) {
            @Override public NodeVisitor child(String ns, String name) {
                return visit(super.child(ns, name), name);
            }
            @Override public void attr(String ns, String name, int id, int type, Object value) {
                if ("manifest".equals(element) && "package".equals(name)) {
                    if (!ORIGINAL.equals(value)) throw new IllegalArgumentException("Unexpected input package");
                    value = TEST;
                } else if ("uses-sdk".equals(element) && "minSdkVersion".equals(name)) {
                    value = Math.max((Integer)value, 33);
                } else if ("label".equals(name) && COMPONENTS.contains(element)) {
                    value = "SquareGlass Test";
                    type = AxmlVisitor.TYPE_STRING;
                } else if (value instanceof String s) {
                    if ("authorities".equals(name) || "permission".equals(name)
                            || (Set.of("permission", "uses-permission").contains(element) && "name".equals(name))) {
                        value = s.replace(ORIGINAL, TEST);
                    } else if (COMPONENTS.contains(element)
                            && Set.of("name", "targetActivity", "parentActivityName").contains(name)
                            && s.startsWith(".")) {
                        value = ORIGINAL + s;
                    }
                }
                super.attr(ns, name, id, type, value);
            }
        };
    }

    public static void main(String[] args) throws Exception {
        var writer = new AxmlWriter();
        new AxmlReader(Files.readAllBytes(Path.of(args[0]))).accept(new AxmlVisitor(writer) {
            @Override public NodeVisitor child(String ns, String name) {
                return visit(super.child(ns, name), name);
            }
        });
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
