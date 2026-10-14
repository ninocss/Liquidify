/* GPL-3.0-or-later. Mirrors the Manager's upstream fuzzy-context application. */
import java.nio.file.*;
import java.util.*;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.AbstractDelta;

public class UpstreamPatchCheck {
    public static void main(String[] args) throws Exception {
        Path base=Path.of(args[0]), output=Path.of(args[2]);
        Map<String,List<String>> changed=new HashMap<>();
        for(String name:Files.readAllLines(Path.of(args[1]))) {
            var lines=Files.readAllLines(Path.of(name));
            List<Integer> starts=new ArrayList<>();
            for(int i=0;i<lines.size();i++) if(lines.get(i).startsWith("--- a/")) starts.add(i);
            for(int i=0;i<starts.size();i++) {
                int start=starts.get(i), end=i+1<starts.size()?starts.get(i+1):lines.size();
                String target=lines.get(start).substring(6);
                var content=new ArrayList<>(changed.containsKey(target)?changed.get(target):Files.readAllLines(base.resolve(target)));
                var patch=UnifiedDiffUtils.parseUnifiedDiff(lines.subList(start,end));
                var deltas=new ArrayList<>(patch.getDeltas());
                deltas.sort(Comparator.comparingInt((AbstractDelta<String> d)->d.getSource().getPosition()).reversed());
                for(var delta:deltas) {
                    var before=delta.getSource().getLines(); int hint=delta.getSource().getPosition(), at=-1;
                    for(int distance=0;distance<=content.size() && at<0;distance++) {
                        for(int pos:new int[]{hint+distance,hint-distance}) {
                            if(pos>=0 && pos+before.size()<=content.size() && content.subList(pos,pos+before.size()).equals(before)) {at=pos;break;}
                        }
                    }
                    if(at<0) throw new IllegalStateException(Path.of(name).getFileName()+": context missing in "+target+" at "+(hint+1));
                    content.subList(at,at+before.size()).clear(); content.addAll(at,delta.getTarget().getLines());
                }
                changed.put(target,content);
            }
        }
        for(var entry:changed.entrySet()) {
            Path file=output.resolve(entry.getKey()); Files.createDirectories(file.getParent()); Files.write(file,entry.getValue());
        }
        System.out.println("Applied upstream patch plan to "+changed.size()+" native classes");
    }
}
