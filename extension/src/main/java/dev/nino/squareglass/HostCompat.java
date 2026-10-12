/* GPL-3.0-or-later. Small Java-only bridges avoid binding to renamed Kotlin internals. */
package dev.nino.squareglass;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NoSuchElementException;

public final class HostCompat {
    private HostCompat() {}
    public static void throwIndexOverflow() { throw new ArithmeticException("Index overflow"); }
    public static final java.nio.charset.Charset UTF_8=java.nio.charset.StandardCharsets.UTF_8;
    public static void throwCountOverflow() { throw new ArithmeticException("Count overflow"); }
    public static int collectionSizeOrDefault(Iterable<?> values,int fallback) {return values instanceof java.util.Collection?((java.util.Collection<?>)values).size():fallback;}
    public static Object firstOrNull(List<?> values){return values.isEmpty()?null:values.get(0);}
    public static int getLastIndex(List<?> values){return values.size()-1;}
    public static List<?> emptyList(){return Collections.emptyList();}
    public static java.util.Set<?> emptySet(){return Collections.emptySet();}
    public static java.util.Set<?> toSet(Iterable<?> values){java.util.Set<Object> out=new java.util.LinkedHashSet<>();for(Object value:values)out.add(value);return out;}
    public static List<?> distinct(Iterable<?> values){return new ArrayList<>(toSet(values));}
    public static List<?> take(Iterable<?> values,int count){if(count<0)throw new IllegalArgumentException();List<Object> out=new ArrayList<>();for(Object value:values){if(out.size()==count)break;out.add(value);}return out;}
    public static java.util.Set<?> plus(java.util.Set<?> values,Object value){java.util.Set<Object> out=new java.util.LinkedHashSet<>(values);out.add(value);return out;}
    public static java.util.Set<?> minus(java.util.Set<?> values,Object value){java.util.Set<Object> out=new java.util.LinkedHashSet<>(values);out.remove(value);return out;}
    public static boolean isBlank(CharSequence value){for(int i=0;i<value.length();i++)if(!Character.isWhitespace(value.charAt(i)) && !Character.isSpaceChar(value.charAt(i)))return false;return true;}
    public static CharSequence trim(CharSequence value){int start=0,end=value.length();while(start<end && (Character.isWhitespace(value.charAt(start))||Character.isSpaceChar(value.charAt(start))))start++;while(end>start && (Character.isWhitespace(value.charAt(end-1))||Character.isSpaceChar(value.charAt(end-1))))end--;return value.subSequence(start,end);}
    public static String take(String value,int count){if(count<0)throw new IllegalArgumentException();return value.substring(0,Math.min(value.length(),count));}
    public static String padStart(String value,int length,char padding){StringBuilder out=new StringBuilder();for(int i=value.length();i<length;i++)out.append(padding);return out.append(value).toString();}
    public static boolean startsWith$default(String value,String prefix,boolean ignoreCase,int mask,Object unused){if((mask&2)!=0)ignoreCase=false;return value.regionMatches(ignoreCase,0,prefix,0,prefix.length());}
    @SuppressWarnings("unchecked")
    public static String joinToString$default(Iterable<?> values,CharSequence separator,CharSequence prefix,CharSequence postfix,int limit,CharSequence truncated,kotlin.jvm.functions.Function1<Object,CharSequence> transform,int mask,Object unused){
        if((mask&1)!=0)separator=", ";if((mask&2)!=0)prefix="";if((mask&4)!=0)postfix="";if((mask&8)!=0)limit=-1;if((mask&16)!=0)truncated="...";if((mask&32)!=0)transform=null;
        StringBuilder out=new StringBuilder(prefix);int count=0;for(Object value:values){if(count++>0)out.append(separator);if(limit>=0 && count>limit){out.append(truncated);break;}out.append(transform==null?String.valueOf(value):transform.invoke(value));}return out.append(postfix).toString();
    }
    public static byte[] readBytes(java.io.InputStream stream)throws java.io.IOException {java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=stream.read(buffer))!=-1){if(out.size()+count>16777216)throw new java.io.IOException("Input exceeds limit");out.write(buffer,0,count);}return out.toByteArray();}
    public static void closeFinally(java.io.Closeable value,Throwable failure)throws java.io.IOException {if(value==null)return;try{value.close();}catch(Throwable error){if(failure!=null)failure.addSuppressed(error);else if(error instanceof java.io.IOException)throw(java.io.IOException)error;else if(error instanceof RuntimeException)throw(RuntimeException)error;else throw new java.io.IOException(error);}}
    public static void checkNotNull(Object value) { Objects.requireNonNull(value); }
    public static void checkNotNull(Object value, String message) { Objects.requireNonNull(value, message); }
    public static void checkNotNullParameter(Object value, String name) {
        if (value == null) throw new NullPointerException("Parameter must not be null: " + name);
    }
    public static void checkNotNullExpressionValue(Object value, String expression) {
        if (value == null) throw new NullPointerException(expression + " must not be null");
    }
    public static boolean areEqual(Object a, Object b) { return Objects.equals(a, b); }
    public static boolean areEqual(Float a, float b) { return a != null && a.floatValue() == b; }
    public static int coerceAtLeast(int value, int minimum) { return Math.max(value, minimum); }
    public static float coerceAtLeast(float value, float minimum) { return Math.max(value, minimum); }
    public static int coerceIn(int value, int minimum, int maximum) {
        if (minimum > maximum) throw new IllegalArgumentException("Empty range");
        return Math.max(minimum, Math.min(maximum, value));
    }
    public static float coerceAtMost(float value, float maximum) { return Math.min(value, maximum); }
    public static Float boxFloat(float value) { return Float.valueOf(value); }
    public static Integer boxInt(int value) { return Integer.valueOf(value); }
    public static Boolean boxBoolean(boolean value) { return Boolean.valueOf(value); }
    public static Object getOrNull(List<?> list,int index) {return index>=0 && index<list.size()?list.get(index):null;}
    public static Object nullOutSpilledVariable(Object value) { return null; }
    public static Object getCOROUTINE_SUSPENDED() { return kotlin.coroutines.intrinsics.CoroutineSingletons.COROUTINE_SUSPENDED; }
    public static int roundToInt(float value) {
        if (Float.isNaN(value)) throw new IllegalArgumentException("Cannot round NaN value.");
        return Math.round(value);
    }
    public static Object first(List<?> list) {
        if (list.isEmpty()) throw new NoSuchElementException("List is empty.");
        return list.get(0);
    }
    public static List<?> listOf(Object item) { return Collections.singletonList(item); }
    public static List<?> listOf(Object[] items) { return java.util.Arrays.asList(items); }
    public static List<?> toList(Iterable<?> source) {
        List<Object> list = new ArrayList<>();
        for (Object item : source) list.add(item);
        return list;
    }
    public static float coerceIn(float value, float minimum, float maximum) {
        if (minimum > maximum) throw new IllegalArgumentException("Empty range: " + minimum + ".." + maximum);
        return value < minimum ? minimum : value > maximum ? maximum : value;
    }
}
