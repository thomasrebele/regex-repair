package fr.telecom_paristech.dbweb.regexrepair.iface;

/** Represents a (repaired) regex, and the time it took to generate it */
public class TimedResult {

  /** Resulting regex */
  public String expr;

  /** Time actually spent getting the match results (doing matching, fetching from cache) */
  public long realTimeMatchPhase = 0;

  /** Time spent doing the matching */
  public long virtualTimeMatchPhase = 0;
}