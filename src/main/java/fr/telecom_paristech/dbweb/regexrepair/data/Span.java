package fr.telecom_paristech.dbweb.regexrepair.data;

/**
 * A span represents a list of indices from a start index (inclusive) to an end index (exclusive) of a sublist/substring/...
 * 
 * @author Thomas Rebele
 */
public class Span implements Comparable<Span> {

  public int start, end;

  public Span() {

  }

  public Span(int start, int end) {
    this.start = start;
    this.end = end;
  }

  @Override
  /** Sort first by start index, then by end index
   * Empty spans are out of order
   * e.g., (1,2) &lt; (2,2) &lt; (2,4) &lt; (2,3)
   */
  public int compareTo(Span o) {
    if (o == null) {
      return -1;
    }
    int startCmp = Integer.compare(start, o.start);
    if (startCmp != 0) {
      return startCmp;
    }
    // treat empty spans out of order
    // e.g. (1,2) < (2,2) < (2,4) < (2,3)
    if (end == start || o.end == start) {
      return Integer.compare(end, o.end);
    }
    return -Integer.compare(end, o.end);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1234567891;
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || !(other instanceof Span)) {
      return false;
    }
    Span o = (Span) other;
    boolean startEq = start == o.start;
    boolean endEq = end == o.end;
    return startEq && endEq;
  }

  /** Whether this instance contains all indexes of other span */
  public boolean contains(Span o) {
    if (o == null) {
      return false;
    }
    return start <= o.start && o.end <= end;
  }

  @Override
  public String toString() {
    String range = start + "," + end;
    return "⟦" + range + "⟧";
  }

  public int length() {
    return end - start;
  }

}
