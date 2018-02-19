package fr.telecom_paristech.dbweb.regexrepair.iface;

/** Feedback gives the algorithm the possibility to check the qualitity of intermediate or final regexes */
public interface Feedback {

  /** Whether the regex has an acceptable quality */
  boolean allow(String regex);

  /** how much time feedback would take on the first call (i.e. without caches) */
  default long virtualTimeInNanoseconds() {
    return 0;
  }

  /** how long the call took actually */
  default long realTimeInNanoseconds() {
    return 0;
  }

  /** how often the feedback was called */
  default long count() {
    return 0;
  }

  /** A feedback which allows all modifications */
  public static Feedback ALWAYS_TRUE = (s) -> true;
}
