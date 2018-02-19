package fr.telecom_paristech.dbweb.regexrepair.adaptive;

import java.util.Map;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;

/** Helper class that tracks one branch of a (modified) regex tree, and its corresponding original tree. */
class Branch {

  Map<Expr, Expr> newToOld = null;

  public Branch(Map<Expr, Expr> map) {
    this.newToOld = map;
  }

  /** Map a node of the new branch to one of the original branch */
  Expr getOrigExpr(Expr e) {
    return newToOld == null ? e : newToOld.get(e);
  }

  /**
   * Apply map of this branch to another branch. 
   * Given this branch maps A to B, and newerToNew maps C to A, the result will map C to B
   * @param newerToNew updated by this function
   */
  public void concatenateMap(Map<Expr, Expr> newerToNew) {
    for (Expr e : newerToNew.keySet()) {
      Expr redirect = getOrigExpr(newerToNew.get(e));
      if (redirect != null && redirect != e) {
        newerToNew.put(e, redirect);
      }
    }
  }
}