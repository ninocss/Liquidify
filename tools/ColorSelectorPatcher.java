/* Square Glass patch tooling. GPL-3.0-or-later. */
package dev.nino.squareglass.patching;

import pxb.android.axml.*;

/** Rewrite only a known theme reference in a fingerprinted native selector. */
public final class ColorSelectorPatcher {
    public static byte[] patch(byte[] input, int attributeId, int expectedType, int fromColor, int toColor, int expectedCount) throws java.io.IOException {
        var writer=new AxmlWriter();
        int[] count={0};
        new AxmlReader(input).accept(new AxmlVisitor(writer) {
            @Override public NodeVisitor child(String ns, String name) {
                return visit(super.child(ns,name));
            }
            private NodeVisitor visit(NodeVisitor parent) {
                return new NodeVisitor(parent) {
                    @Override public NodeVisitor child(String ns,String name) { return visit(super.child(ns,name)); }
                    @Override public void attr(String ns,String name,int resourceId,int type,Object value) {
                        if (resourceId==attributeId && type==expectedType && value instanceof Integer && ((Integer)value)==fromColor) {
                            value=toColor; type=1; count[0]++;
                        }
                        super.attr(ns,name,resourceId,type,value);
                    }
                };
            }
        });
        if (expectedCount<1 || count[0]!=expectedCount) throw new IllegalArgumentException("Unexpected theme XML match count: "+count[0]);
        return writer.toByteArray();
    }
}
