package fr.telecom_paristech.dbweb.regexrepair.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A class representing a substring of a string.
 */
public class TextSpan extends Span {

  public String text;

  public TextSpan() {

  }

  public TextSpan(String text, int start, int end) {
    super(start, end);
    this.text = text;
  }

  public TextSpan(String text) {
    this(text, 0, text.length());
  }

  @Override
  public int hashCode() {
    return super.hashCode() + ((text == null) ? 0 : 1234567891 * text.hashCode());
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || !(other instanceof TextSpan)) {
      return false;
    }
    TextSpan o = (TextSpan) other;
    boolean textEq = text == null ? text == o.text : text.equals(o.text);
    return textEq && super.equals(other);
  }

  @Override
  public String toString() {
    String str = spanString();
    if ("".equals(str)) {
      str = " ";
    }
    str = str + super.toString();
    str = str.replace("\n", " \u21B5 ");
    return str;
  }

  public String spanString() {
    if (start < 0) {
      return "(" + start + "-" + end + ", invalid)";
    }
    if (text != null && end > text.length()) {
      return "(" + start + "-" + end + ", invalid)";
    }
    if (end < start) {
      return "(" + start + "-" + end + ", invalid)";
    }
    return text == null ? "" : text.substring(start, end);
  }

  public TextSpan startSpan() {
    return new TextSpan(text, start, start);
  }

  /** Get character with position starting at span string */
  public char charAt(int pos) {
    return charAt(pos, true);
  }

  /** Like {@link #charAt(int)}, but start from end of span, if 'forward' is false */
  public char charAt(int pos, boolean forward) {
    if (forward) {
      pos = start + pos;
      if (pos >= end) {
        return (char) -1;
      }
    } else {
      pos = end - pos - 1;
      if (pos < start) {
        return (char) -1;
      }
    }
    return text.charAt(pos);
  }

  /** Get index of character, starting at span start (if 'forward'), otherwise starting at span end */
  public int indexOf(char c, boolean forward) {
    int idx;
    if(forward) {
      idx = text.indexOf(c, start);
    } else {
      idx = end - 1 - text.lastIndexOf(c, end - 1);
    }
    idx -= start;
    return idx < 0 || idx >= length() ? -1 : idx;
  }

  /** Remove whitespace at beginning / end of span, without changing underlying string */
  public TextSpan trim() {
    TextSpan r = new TextSpan();
    r.text = text;
    String trimmed = spanString().trim();
    r.start = r.text.indexOf(trimmed, r.start);
    r.end = r.start + trimmed.length();
    return r;
  }

  /** Takes a list of spans on the same string, and returns all spans of the string that are not covered by the list */
  public List<TextSpan> complement(List<TextSpan> list) {
    List<TextSpan> result = new ArrayList<>();

    Set<Integer> borders = new TreeSet<>();
    for (TextSpan s : list) {
      borders.add(s.start);
      borders.add(s.end);
    }
    borders.add(end);

    String base = text;
    int tmpStart = start;
    for (Integer act : borders) {
      if (act > tmpStart) {
        boolean hasOverlap = false;
        for (Span s : list) {
          if (s.contains(new Span(tmpStart, act))) {
            hasOverlap = true;
          }
        }
        if (!hasOverlap) {
          TextSpan leftOver = new TextSpan(base, tmpStart, act);
          result.add(leftOver);
        }
      }
      tmpStart = act;
    }

    return result;
  }

  /** Returns all spans of 'text' that have the string 'substr' */
  public static List<TextSpan> fromSubstring(String text, String substr) {
    List<TextSpan> result = new ArrayList<>();
    int idx = 0;
    while (idx != -1) {
      idx = text.indexOf(substr, idx);
      if (idx != -1) {
        result.add(new TextSpan(text, idx, idx += substr.length()));
      }
    }
    return result;
  }

  public static void main(String[] args) {
    String t = "abcdefg";
    TextSpan a = new TextSpan(t, 0, t.length()), b = new TextSpan(t, 2, 3), c = new TextSpan(t, 3, 6);
    System.out.println(a.complement(Arrays.asList(b, c)));
  }

}