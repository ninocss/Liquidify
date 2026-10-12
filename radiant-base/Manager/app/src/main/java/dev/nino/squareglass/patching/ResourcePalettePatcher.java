/* Square Glass patch tooling. GPL-3.0-or-later. */
package dev.nino.squareglass.patching;

import com.google.devrel.gmscore.tools.apk.arsc.*;
import java.nio.*;
import java.io.ByteArrayOutputStream;
import java.util.*;

/** Shared by the desktop build and the Manager fork. Keeps every resource ID. */
public final class ResourcePalettePatcher {
    public static final class ColorChange {
        public final int id, expectedData, light, dark;
        public final String name, expectedType;
        public ColorChange(int id, String name, String expectedType, int expectedData, int light, int dark) {
            this.id=id; this.name=name; this.expectedType=expectedType;
            this.expectedData=expectedData; this.light=light; this.dark=dark;
        }
    }

    public static byte[] patch(byte[] input, List<ColorChange> changes, int mode) {
        if (mode < 0 || mode > 2 || changes.isEmpty()) throw new IllegalArgumentException("Invalid palette request");
        var file = new BinaryResourceFile(input);
        var table = (ResourceTableChunk) file.getChunks().get(0);
        if (table.getPackages().size()!=1) throw new IllegalArgumentException("Expected one resource package");
        var pkg = table.getPackages().iterator().next();
        TypeChunk defaults=null, night=null;
        for (var type : pkg.getTypeChunks()) {
            if (!type.getTypeName().equals("color")) continue;
            if (type.getConfiguration().isDefault()) defaults=type;
            if (type.getConfiguration().toString().equals("night")) night=type;
        }
        if (defaults==null) throw new IllegalArgumentException("Missing default colors");
        var indices = new HashSet<Integer>();
        for (var change : changes) {
            if ((change.id >>> 24)!=pkg.getId() || ((change.id>>>16)&255)!=defaults.getId())
                throw new IllegalArgumentException("Color ID outside target type");
            int index=change.id&65535;
            if (!indices.add(index)) throw new IllegalArgumentException("Duplicate color ID");
            var entry=defaults.getEntry(index);
            if (entry==null || entry.isComplex() || !entry.key().equals(change.name)
                    || !entry.value().type().name().equals(change.expectedType) || entry.value().data()!=change.expectedData)
                throw new IllegalArgumentException("Unexpected resource: "+change.name);
        }
        boolean addNight=night==null;
        if (addNight) {
            // ResTable_type starts with a 20-byte header; uiMode is byte 29
            // of ResTable_config. Keep all other qualifiers and entry IDs.
            byte[] raw=defaults.toByteArray();
            if (raw.length<50 || ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getInt(20)<32)
                throw new IllegalArgumentException("Unknown resource configuration format");
            raw[49]=(byte)((raw[49]&~0x30)|0x20);
            night=(TypeChunk)Chunk.newInstance(ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN),pkg);
            if (!night.getConfiguration().toString().equals("night")) throw new IllegalArgumentException("Invalid night configuration");
            night.overrideEntries(new HashMap<>());
            pkg.appendChunk(night);
        }
        for (var change : changes) {
            int index=change.id&65535;
            var original=defaults.getEntry(index);
            night.overrideEntry(index, new TypeChunk.Entry(original.headerSize(), original.flags(), original.keyIndex(),
                new BinaryResourceValue(BinaryResourceValue.Type.INT_COLOR_ARGB8, mode==1 ? change.light : change.dark),
                null, original.parentEntry(), night));
            // getEntry() parses a fresh view of the original byte buffer. Mutating
            // that object's value alone is discarded by TypeChunk serialization.
            defaults.overrideEntry(index, new TypeChunk.Entry(original.headerSize(), original.flags(), original.keyIndex(),
                new BinaryResourceValue(BinaryResourceValue.Type.INT_COLOR_ARGB8, mode==2 ? change.dark : change.light),
                null, original.parentEntry(), defaults));
        }
        var replacements=new HashMap<Integer,byte[]>();
        replacements.put(defaults.getOriginalOffset(),defaults.toByteArray());
        if (!addNight) replacements.put(night.getOriginalOffset(),night.toByteArray());
        // Re-encoding a whole stock table needlessly rewrites its large string
        // pools. Keep every unrelated chunk byte-for-byte, including string pools.
        byte[] output=rewrite(input,0,input.length,pkg.getId(),defaults.getId(),indices,
                replacements,addNight ? night.toByteArray() : null);
        // Parse our serialized result again before returning it to a packager.
        new BinaryResourceFile(output);
        return output;
    }

    private static byte[] rewrite(byte[] bytes, int start, int end, int packageId, int typeId, Set<Integer> indices,
            Map<Integer,byte[]> replacements, byte[] appendNight) {
        ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var output=new ByteArrayOutputStream();
        for (int pos=start; pos<end;) {
            if (pos+8>end) throw new IllegalArgumentException("Truncated resource chunk");
            int type=b.getShort(pos)&65535, header=b.getShort(pos+2)&65535, size=b.getInt(pos+4);
            if (header<8 || size<header || size>end-pos) throw new IllegalArgumentException("Invalid resource chunk bounds");
            byte[] chunk=replacements.get(pos);
            if (chunk!=null) {
                output.write(chunk,0,chunk.length); pos+=size; continue;
            }
            chunk=Arrays.copyOfRange(bytes,pos,pos+size);
            boolean table=type==0x0002, targetPackage=type==0x0200 && b.getInt(pos+8)==packageId;
            if (table || targetPackage) {
                byte[] children=rewrite(bytes,pos+header,pos+size,packageId,typeId,indices,replacements,appendNight);
                var rebuilt=new ByteArrayOutputStream();
                rebuilt.write(chunk,0,header);
                rebuilt.write(children,0,children.length);
                if (targetPackage && appendNight!=null) rebuilt.write(appendNight,0,appendNight.length);
                byte[] result=rebuilt.toByteArray();
                ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).putInt(4,result.length);
                // Key/type pools precede the modified type chunks in the pinned APK.
                // Refuse a future layout requiring pool-offset relocation.
                if (targetPackage) {
                    int firstReplacement=Collections.min(replacements.keySet())-pos;
                    if (b.getInt(pos+260)>=firstReplacement || b.getInt(pos+268)>=firstReplacement)
                        throw new IllegalArgumentException("Unsupported resource pool order");
                }
                chunk=result;
            } else if (type==0x0202 && (b.get(pos+8)&255)==typeId) {
                int count=b.getInt(pos+12);
                if (header+count*4>size) throw new IllegalArgumentException("Invalid TypeSpec flags");
                for (int index:indices) {
                    if (index>=count) throw new IllegalArgumentException("Color outside TypeSpec");
                    int offset=header+index*4;
                    ByteBuffer cb=ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
                    cb.putInt(offset,cb.getInt(offset)|0x0200);
                }
            }
            output.write(chunk,0,chunk.length);
            pos+=size;
        }
        return output.toByteArray();
    }
}
