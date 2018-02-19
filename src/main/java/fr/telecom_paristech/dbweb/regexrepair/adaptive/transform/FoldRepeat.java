package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import java.util.ArrayList;
import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;

/**
 * Combine repeated expressions to one.
 * Example: 
 *  \d\d\d\d\d* becomes \d{4,}
 *  \d\d?\d?\d* becomes \d+
 *  a{2}a{4}aaa becomes a{9}
 */
public class FoldRepeat extends CopyExpr {

  private List<Repeat> embed(List<Expr> l) {
    List<Repeat> result = new ArrayList<>();
    for (Expr e : l) {
      if (e instanceof Repeat) {
        result.add((Repeat) apply(e, null));
      } else {
        result.add(new Repeat(apply(e, null), 1, 1));
      }
    }
    return result;
  }

  private List<Repeat> combine(List<Repeat> l) {
    // store subexpression which we try to combine with the following expressions
    String regex = null;
    // store result
    Repeat rep = null;
    List<Repeat> result = new ArrayList<>();
    for (int i = 0; i < l.size(); i++) {
      Repeat c = l.get(i);
      Expr child = c.getChildren().get(0);
      if (child.toRegexString().equals(regex)) {
        rep.addToMin(c.getMin());
        rep.addToMax(c.getMax());
      } else {
        result.add(rep);
        regex = child.toRegexString();
        rep = c;
      }
    }
    result.add(rep);
    return result;
  }

  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (expr instanceof Conc) {
      Conc n = new Conc();
      List<Repeat> l = embed(((Conc) expr).getChildren());
      List<Repeat> newL = combine(l);

      for (Repeat e : newL) {
        add(n, e);
      }

      return addTo(n, parent);
    } else {
      return super.apply(expr, parent);
    }
  }

  /**
   * Adds expression r as a child to a concatenation.
   * It checks for null or expr{1} repetitions
   * @param c
   * @param r
   */
  private void add(Conc c, Repeat r) {
    if (r == null) {
      return;
    }

    // check wether a simply concatenation leads to less characters (e.g. tt instead of t{2})
    boolean asConc = false;
    if (r.getMin() == r.getMax()) {
      int lenRep = r.toRegexString().length();
      int lenConc = r.getChildren().get(0).toRegexString().length() * r.getMax();
      if (lenConc <= lenRep) {
        asConc = true;
      }
    }
    if (asConc) {
      for (int i = 0; i < r.getMax(); i++) {
        c.addChild(r.getChildren().get(0));
      }
    } else {
      c.addChild(r);
    }
  }

  public static void main(String[] args) {
    Expr e = RegexParser.parse("aaaaaaaa");
    e = RegexParser.parse("\\d\\d\\d\\d\\d*");
    System.out.println(e.toRegexString());
    System.out.println(e.print());
    e = new FoldRepeat().apply(e);
    System.out.println(e.toRegexString());
    System.out.println(e.print());
  }

}
