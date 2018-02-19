package fr.telecom_paristech.dbweb.regexrepair.adaptive;

import java.util.Set;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.ExprPos;

/**
 * Represents a pair of "char to regex leaf" tuples of a matching, that are not "consecutive"
 * 
 * Implementation note: don't define hashCode / equals, as we use a HashMap<Gap,...> which requires instances as keys
 */
class Gap {

  /** Start of this gap in the regex tree */
  public final ExprPos leaf1;

  /** End of this gap in the regex tree */
  public final ExprPos leaf2;

  /** The substring that we need to add to one of the primary alternatives. Can be originalToAdd or "" */
  String toAdd;

  /** The substring of the missing word that corresponds to this gap. */
  final String originalToAdd;

  /** The missing word (the one that we want to add to the regex) that causes this gap to appear. */
  final String missingWord;

  /** Can choose one primary alt for the replacement, need to make the others optional. */
  final Set<Alt> primaryAlts;

  /** Need to make all secondary alts optional. */
  final Set<Alt> secondaryAlts;

  /** Constructor */
  Gap(ExprPos leaf1, ExprPos leaf2, String string, String origToAdd, String missingWord, Set<Alt> alts, Set<Alt> secondaryAlts) {
    this.toAdd = string;
    this.originalToAdd = origToAdd;
    this.leaf1 = leaf1;
    this.leaf2 = leaf2;
    this.missingWord = missingWord;
    this.primaryAlts = alts;
    this.secondaryAlts = secondaryAlts;
  }

  /** Create a copy with this new information */
  Gap copy(ExprPos leaf1, ExprPos leaf2, String string) {
    return new Gap(leaf1, leaf2, string, originalToAdd, missingWord, primaryAlts, secondaryAlts);
  }

  @Override
  public String toString() {
    return "fix gap between " + leaf1 + " and " + leaf2 + " with '" + toAdd + "'" + " originally '" + originalToAdd + "' for word '" + missingWord
        + "'";
  }
}