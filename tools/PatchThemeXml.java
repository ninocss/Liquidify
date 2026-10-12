import java.nio.file.*;
import dev.nino.squareglass.patching.ColorSelectorPatcher;
public class PatchThemeXml {
    static int hex(String s) { return (int)Long.parseLong(s,16); }
    public static void main(String[] args) throws Exception {
        byte[] output=ColorSelectorPatcher.patch(Files.readAllBytes(Path.of(args[0])),hex(args[2]),
            Integer.parseInt(args[3]),hex(args[4]),hex(args[5]),Integer.parseInt(args[6]));
        Files.write(Path.of(args[1]),output);
    }
}
