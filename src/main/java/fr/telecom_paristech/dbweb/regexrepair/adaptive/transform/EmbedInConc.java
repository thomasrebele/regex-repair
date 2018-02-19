package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;

/** Wrap all non concatenation nodes in a concatenation with one child */
public class EmbedInConc extends CopyExpr {

  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (!(expr instanceof Conc) && !(parent instanceof Conc)) {
      Conc c = new Conc();
      addTo(c, parent);
      apply(expr, c);
      return c;
    }

    return super.apply(expr, parent);
  }

  public static void main(String[] args) {
    Expr e = RegexParser.parse("(a|b)*");
    //e = RegexParser.parse("a");
    System.out.println(e.toRegexString());
    System.out.println(e.print());
    e = new EmbedInConc().apply(e);
    System.out.println(e.toRegexString());
    System.out.println(e.print());
  }

}
