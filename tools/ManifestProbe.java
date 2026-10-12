/* LOCAL TEST ONLY: allow the emulator shell to launch TIDAL's internal activity. */
import java.nio.file.*;
import pxb.android.axml.*;

public class ManifestProbe {
    public static void main(String[] args) throws Exception {
        var reader = new AxmlReader(Files.readAllBytes(Path.of(args[0])));
        var writer = new AxmlWriter();
        reader.accept(new AxmlVisitor(writer) {
            @Override public NodeVisitor child(String ns, String name) {
                return new NodeVisitor(super.child(ns, name)) {
                    @Override public NodeVisitor child(String ns, String name) {
                        return new NodeVisitor(super.child(ns, name)) {
                            @Override public NodeVisitor child(String ns, String name) {
                                boolean isActivity = "activity".equals(name);
                                return new NodeVisitor(super.child(ns, name)) {
                                    boolean target = false;
                                    boolean exported = false;
                                    @Override public void attr(String ns, String name, int id, int type, Object value) {
                                        if (isActivity && "name".equals(name) && "com.aspiro.wamp.MainActivity".equals(value)) target = true;
                                        if (isActivity && "exported".equals(name)) {
                                            exported = true;
                                            if (target) value = true;
                                        }
                                        super.attr(ns, name, id, type, value);
                                    }
                                    @Override public void end() {
                                        if (target && !exported) super.attr("http://schemas.android.com/apk/res/android", "exported", 0x01010010, AxmlVisitor.TYPE_INT_BOOLEAN, true);
                                        super.end();
                                    }
                                };
                            }
                        };
                    }
                };
            }
        });
        Files.write(Path.of(args[1]), writer.toByteArray());
    }
}
