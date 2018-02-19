package fr.telecom_paristech.dbweb.regexrepair.iface;

import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;

/** Represents correspondences between each char of string and leaves of regex */
public class Matching {

  /** Every character is represented by a list item. 'null' represents unmatched characters */
  public List<Expr> matches;

  /** Original string */
  public String str;

}
