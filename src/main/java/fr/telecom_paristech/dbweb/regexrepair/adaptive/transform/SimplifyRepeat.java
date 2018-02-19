package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;

/** Simplify nested quantifiers
 * E.g. (a?)? becomes a?
 * (b{3}){4} becomes b{12}
 */
public class SimplifyRepeat extends CopyExpr {

  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (expr instanceof Repeat && parent instanceof Repeat) {
      boolean combine = false;
      Repeat cr = (Repeat) expr, pr = (Repeat) parent;
      combine |= cr.getMin() == cr.getMax() && pr.getMin() == pr.getMax();
      combine |= cr.getMin() == 0;

      if (combine) {
        pr.setMin(cr.getMin() * pr.getMin());
        pr.setMax(cr.getMax() * pr.getMax());
        return apply(cr.getChild(), parent);
      }
    }

    return super.apply(expr, parent);
  }

  public static void main(String[] args) {
    Expr e = RegexParser.parse("((a)?)? (b{3}){4}");
    System.out.println(e.toRegexString());
    System.out.println(e.print());

    e = new SimplifyRepeat().apply(e);
    System.out.println(e.toRegexString());
    System.out.println(e.print());
  }

}
