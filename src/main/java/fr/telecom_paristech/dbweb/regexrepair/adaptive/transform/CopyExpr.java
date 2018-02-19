package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import java.util.HashMap;
import java.util.Map;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;

/** Base class for other transformations.
 * Creates a copy of all nodes.
 * Tracks a bijection between old and new nodes.
 */
public class CopyExpr {

  Map<Expr, Expr> oldToNew = new HashMap<>();

  Map<Expr, Expr> newToOld = new HashMap<>();

  public Expr apply(Expr expr) {
    return apply(expr, null);
  }

  public Map<Expr, Expr> getOldToNewMap() {
    return oldToNew;
  }

  public Map<Expr, Expr> getNewToOldMap() {
    return newToOld;
  }

  /**
   * Applies transformation (default: copy) to expr.
   * 
   * Contract:
   * - parent has child substituting expr
   * - method generates new objects, i.e. expr and parent have no common decendants
   * 
   * @return parent == null ? expr : parent
   */
  protected Expr apply(Expr expr, AggExpr parent) {
    Expr copy = copy(expr);

    if (copy instanceof AggExpr) {
      AggExpr ae = (AggExpr) copy;
      ae.clearChildren();
      for (Expr child : ((AggExpr) expr).getChildren()) {
        apply(child, ae);
      }
    }

    return addTo(copy, parent);
  }

  /** Try to add expr to parent if exists.
   * @return parent if possible, otherwise expr */
  protected Expr addTo(Expr expr, Expr parent) {
    if (parent != null) {
      ((AggExpr) parent).addChild(expr);
      return parent;
    }
    return expr;
  }

  /** Create a copy, and track old/new nodes */
  protected Expr copy(Expr expr) {
    Expr e = expr.clone();
    oldToNew.put(expr, e);
    newToOld.put(e, expr);
    return e;
  }
}
