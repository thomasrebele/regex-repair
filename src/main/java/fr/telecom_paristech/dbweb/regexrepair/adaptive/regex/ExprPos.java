package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat.RepeatCycle;
import fr.telecom_paristech.dbweb.regexrepair.data.Span;
import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;

/**
 * An expression position represents the "state" of a regular expression, after "reading" some characters from the input string.
 * It "points" at the leaf of the regex, that was read most recently. It supports "approximate" reading, i.e. skipping leaves of the regex.
 * Examples:
 * - regex (abc|def), reading ab, then expr pos is (a⟦b⟧c|def)
 * - regex (abc|def){3}, reading b, then expr pos is (a⟦b⟧c|def){3}@0 (i.e. first iteration)
 * - regex (abc|def){3}, reading bb, then expr pos is (a⟦b⟧c|def){3}@1 (i.e. second iteration)
 * - regex (a(b){2}c|def){3}, reading bb, then expr pos is (a⟦b⟧{2}@1 c|def){3}@0 (i.e. second iteration of (b){2}, first iteration of outermost quantifier)
 */
public class ExprPos implements Comparable<ExprPos> {

  /** Logger */
  private final static Logger log = LoggerFactory.getLogger(ExprPos.class);

  /** List of quantifiers around this position, each with an iteration counter. Outermost first, innermost last. */
  private List<RepeatCycle> rep = new ArrayList<>(0);

  /** Expression for which we track the position */
  private Expr expr = null;

  /** All ancestor nodes. Outermost first, innermost last. */
  private List<Expr> ancestors = new ArrayList<>();

  /** Get expression position at the beginning of the expression (before reading anything) */
  public static ExprPos first(Expr expr) {
    return new ExprPos();
  }

  /** Expression position "after" the last leaf/leaves */
  public static ExprPos last(Expr value) {
    return null;
  }

  public static int rawIndexIn(Expr parent, ExprPos pos) {
    if (pos == null) {
      return -1;
    }
    List<Expr> ancestors = Expr.getAncestors(pos.expr);
    int ancestorIdx = ancestors.indexOf(parent);
    if (ancestorIdx == -1) {
      return -1;
    }
    return ((AggExpr) ancestors.get(ancestorIdx)).getChildren().indexOf(ancestors.get(ancestorIdx + 1));
  }

  private ExprPos() {

  }

  public Expr getExpr() {
    return expr;
  }

  @Override
  public String toString() {
    String r = "null";
    if (expr != null) {
      Map<Expr, RepeatCycle> m = new HashMap<>();
      for (RepeatCycle rc : rep) {
        m.put(rc.r, rc);
      }
      r = expr.getRoot().toRegexString((e, s) -> {
        if (e == expr) {
          s = Expr.mark(s);
        }
        if (m.containsKey(e)) {
          s = s + "@" + m.get(e).c + " ";
        }
        return s;
      });
    }

    return r;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof ExprPos)) {
      return false;
    }
    ExprPos o = (ExprPos) other;

    return Objects.equals(expr, o.expr) && Objects.equals(rep, o.rep);
  }

  @Override
  public int hashCode() {
    // use toRegexString to make hashing independent of addr of expr
    return 19 + (expr == null ? 1 : Objects.hashCode(expr.toRegexString(expr))) * 31 + 17 * Objects.hashCode(rep);
  }

  /** Get quantifiers wrapped around this position */
  public List<RepeatCycle> getRepeatCycles() {
    return rep;
  }

  /** Get cycle of outermost quantifier */
  public int getCycle() {
    return rep.size() == 0 ? -1 : rep.get(0).c;
  }

  /** Get outermost quantifier */
  public Repeat getRepeat() {
    return rep.size() == 0 ? null : rep.get(0).r;
  }

  /** Unwraps the position from the outmost repetition */
  public ExprPos child() {
    if (rep.size() == 0) {
      return null;
    }
    ExprPos result = new ExprPos();
    result.expr = expr;
    result.rep = new ArrayList<>(rep.subList(1, rep.size()));
    result.ancestors = ancestors;
    return result;
  }

  /** Get the expression position that follows this expression position, and points at leaf 'next'. */
  public ExprPos advance(Expr next) {
    ExprPos result = new ExprPos();
    result.expr = next;
    result.rep = new ArrayList<>();
    result.ancestors = Expr.getAncestors(next);

    // find first different expr
    List<Repeat> repeats = new ArrayList<>();
    int size = Math.min(this.ancestors.size(), result.ancestors.size());
    int firstDiff = size;
    for (int i = 0; i < size; i++) {
      if (this.ancestors.get(i) != result.ancestors.get(i)) {
        firstDiff = i;
        break;
      }
      if (this.ancestors.get(i) instanceof Repeat) {
        repeats.add((Repeat) this.ancestors.get(i));
      }
    }

    if (size > 0 && firstDiff == 0) {
      throw new UnsupportedOperationException("no common ancestor");
    }

    // handle border cases
    if (size == 0) {
      // advance from first
      if (this.ancestors.size() == 0) {
        result.initRepeats(result.ancestors, 0);
        return result;
      }
      // advance to last
      else {
        return null;
      }
    }

    // advance inside if Conc is a common ancestor
    Expr act = this.ancestors.get(firstDiff - 1);
    if (act instanceof Conc && firstDiff < size) {

      int idx1 = ((Conc) act).getChildren().indexOf(this.ancestors.get(firstDiff));
      int idx2 = ((Conc) act).getChildren().indexOf(result.ancestors.get(firstDiff));

      if (idx1 < idx2) {
        if (repeats.size() > 0) {
          result.rep.addAll(rep.subList(0, repeats.size()));
        }
        result.initRepeats(result.ancestors, firstDiff);
        return result;
      }
    }

    if (repeats.size() == 0) {
      return null;
    }

    // go back until a repeat with incrementable cycle
    int incRepeat;
    for (incRepeat = repeats.size(); incRepeat-- > 0;) {
      int newc = rep.get(incRepeat).c + 1;
      if (newc < rep.get(incRepeat).r.getMax()) {
        break;
      }
    }

    if (incRepeat < 0) {
      return null;
    }

    if (incRepeat > 0) {
      result.rep.addAll(rep.subList(0, incRepeat));
    }

    RepeatCycle old = rep.get(incRepeat);
    result.rep.add(old.r.cycle(old.c + 1));
    result.initRepeats(result.ancestors, result.ancestors.indexOf(old.r) + 1);
    return result;
  }

  /** Initialize 'rep', setting all iterations to 0 */
  private void initRepeats(List<Expr> resultAncestors, int start) {
    for (int j = start; j < resultAncestors.size(); j++) {
      if (resultAncestors.get(j) instanceof Repeat) {
        rep.add(((Repeat) resultAncestors.get(j)).cycle(0));
      }
    }
  }

  @Override
  /** Which expression position comes first ? */
  public int compareTo(ExprPos o) {
    int size = Math.min(this.ancestors.size(), o.ancestors.size());
    int repIdx = 0;
    for (int i = 0; i < size; i++) {
      if (this.ancestors.get(i) != o.ancestors.get(i)) {
        if (i == 0) {
          throw new UnsupportedOperationException("cannot compare expr pos of different expressions");
        }
        List<Expr> children = ((AggExpr) this.ancestors.get(i - 1)).getChildren();
        return Integer.compare(children.indexOf(this.ancestors.get(i)), children.indexOf(o.ancestors.get(i)));
      }
      if (this.ancestors.get(i) instanceof Repeat) {
        int cmp = Integer.compare(rep.get(repIdx).c, o.rep.get(repIdx).c);
        if (cmp != 0) {
          return cmp;
        }
        repIdx++;
      }
    }
    return 0;
  }

  /**
   * {@link #wasSingleStep(ExprPos, ExprPos, Expr)}, with respect to the whole expression
   * @param ep1
   * @param ep2
   * @return boolean
   */
  public static boolean wasSingleStep(ExprPos ep1, ExprPos ep2) {
    return wasSingleStep(ep1, ep2, null);
  }

  /**
   * Checks whether we can go with one step (i.e. reading a single character) from ep1 to ep2.
   * Treats null for ep1 or ep2 as start / end of the expr.
   * 
   * If withinExpr != null, it will treat withinExpr as the root of the expression tree
   * @param ep1
   * @param ep2
   * @param withinExpr
   * @return boolean
   */
  public static boolean wasSingleStep(ExprPos ep1, ExprPos ep2, Expr withinExpr) {
    List<Expr> a1 = ep1 == null ? null : ep1.ancestors, a2 = ep2 == null ? null : ep2.ancestors;
    if (withinExpr != null) {
      // deal with start / end pos
      if (ep1 != null && ep1.expr != null) {
        int idx1 = a1.indexOf(withinExpr);
        if (idx1 == -1) {
          log.error("wasSingleStep: ancestors {}", a1);
          log.error("wasSingleStep: index out of range for ep1, arguments {}, {}, {}", ep1, ep2, withinExpr);
        }
        a1 = a1.subList(idx1, a1.size());
      }
      if (ep2 != null && ep2.expr != null) {
        int idx2 = a2.indexOf(withinExpr);
        if (idx2 == -1) {
          log.error("wasSingleStep: ancestors {}", a2);
          log.error("wasSingleStep: index out of range for ep2, arguments {}, {}, {}", ep1, ep2, withinExpr);
        }
        a2 = a2.subList(idx2, a2.size());
      }
    }
    // Q&D fix for wasSingleStep(start, end); wrong result for expressions accepting the empty word
    Expr ancestor = Tools.nonNull(Tools.get(a1, 0), Tools.get(a2, 0));
    if (ancestor == null) {
      return false;
    }
    boolean result = wasSingleStep(ep1, a1, ep2, a2);
    log.trace("single step {} for {} to {} ", result, ep1, ep2);
    return result;
  }

  /** Helper method for wasSingleStep(...) */
  private static boolean wasSingleStep(ExprPos ep1, List<Expr> a1, ExprPos ep2, List<Expr> a2) {
    Expr e1 = Tools.get(a1, 0), e2 = Tools.get(a2, 0);
    ExprPos np1 = ep1, np2 = ep2;
    log.trace("     rec");
    log.trace("          {} at node {}", ep1, e1);
    log.trace("          {} at node {}", ep2, e2);

    if (e1 == null && e2 == null) {
      return true;
    }
    Expr e = Tools.nonNull(e1, e2);
    if (e1 != null && e2 != null && !e1.getClass().equals(e2.getClass())) {
      throw new UnsupportedOperationException("sth went wrong");
    }

    if (e instanceof Conc) {
      List<Expr> children = ((Conc) e).getChildren();
      Span idx = Tools.getIndices(((Conc) e).getChildren(), Tools.get(a1, 1), Tools.get(a2, 1));
      if (idx.end - idx.start > 0) {
        for (int i = idx.start + 1; i < idx.end; i++) {
          if (!children.get(i).acceptsEmptyWord()) {
            return false;
          }
        }
        boolean left = wasSingleStep(ep1, Tools.tail(a1), null, null);
        boolean right = wasSingleStep(null, null, ep2, Tools.tail(a2));
        return left && right;
      }
    }
    if (e instanceof Repeat) {
      np1 = ep1 == null ? null : ep1.child();
      np2 = ep2 == null ? null : ep2.child();

      int c1 = e1 != null ? ep1.getCycle() : 0;
      int c2 = e2 != null ? ep2.getCycle() : Math.max(((Repeat) e).getMin(), c1);

      int diff = c2 - c1;
      if (diff != 0) {
        if (diff > 1) {
          return false;
        }
        return wasSingleStep(null, null, np2, Tools.tail(a2)) && wasSingleStep(np1, Tools.tail(a1), null, null);
      }
    }

    return wasSingleStep(np1, Tools.tail(a1), np2, Tools.tail(a2));
  }

}
