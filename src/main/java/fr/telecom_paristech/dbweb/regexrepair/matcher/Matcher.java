package fr.telecom_paristech.dbweb.regexrepair.matcher;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.iface.Matching;

/**
 * A Matcher tries to map as many characters of a word to leaves of a regex as possible, so that the "order" of the leaves is respected.
 */
public interface Matcher {

  /** Calculate an approximate match of a string to a regex */
  public Matching match(Expr e, String s);

  /** Name of the matcher algorithm */
  public String info();

}
