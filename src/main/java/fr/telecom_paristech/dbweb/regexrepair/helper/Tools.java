package fr.telecom_paristech.dbweb.regexrepair.helper;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.data.Span;

/** Collection of convenience methods */
public class Tools {

  /** Escape a string */
  public static String asJavaString(String str) {
    String result = "\"" + str.replaceAll("([\\\\‌​\\\"])", "\\\\$1") + "\"";
    return result.replace("\n", "\\n");
  }

  /** Output a string representation of a list, that can be copy-pasted into Java code */
  public static String asJavaString(List<String> str) {
    StringBuilder sb = new StringBuilder();
    sb.append("Arrays.asList(");
    for (int i = 0; i < str.size(); i++) {
      if (i != 0) {
        sb.append(",");
      }
      sb.append(asJavaString(str.get(i)));
    }
    sb.append(")");
    return sb.toString();
  }

  /** Transform an iterator to an iterable */
  public static <T> Iterable<T> iterable(Iterator<T> it) {
    return new Iterable<T>() {

      @Override
      public Iterator<T> iterator() {
        return it;
      }
    };
  }

  /** Return first non-null argument, otherwise null */
  public static <T> T nonNull(T t1, T t2) {
    return t1 != null ? t1 : t2;
  }

  /** Id for an object */
  public static String addr(Object obj) {
    return Integer.toHexString(System.identityHashCode(obj));
  }

  /** Get element at index, otherwise null */
  public static <T> T get(List<T> list, int idx) {
    return list == null || list.size() <= idx ? null : list.get(idx);
  }

  /** Get tail of list, otherwise null */
  public static <T> List<T> tail(List<T> list) {
    return list == null || list.size() == 0 ? null : list.subList(1, list.size());
  }

  /** Find two items in list. 
   * If item1 does not exist, take beginning of list.
   * If item2 does not exist, take end of list.
   */
  public static <T> Span getIndices(List<T> list, T item1, T item2) {
    int idx1 = list.indexOf(item1);
    int idx2 = list.indexOf(item2);
    idx2 = idx2 == -1 ? list.size() : idx2;
    return new Span(idx1, idx2);
  }

  /** Get current time in nano seconds */
  public static long nanos() {
    return System.nanoTime();
  }

  private static DecimalFormat df = new DecimalFormat("#.##");

  /** Transform nano seconds to string (in seconds */
  public static String formatNanoseconds(long nanoseconds) {
    return df.format(nanoseconds / 1e9);
  }

}
