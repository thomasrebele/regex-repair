package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;

/** Convert (...||...) to (...|...)? */
public class AltToRep extends CopyExpr {

  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (expr instanceof Alt) {
      // 
      AggExpr n = new Alt();

      boolean producesEmpty = false;
      for (Expr child : ((Alt) expr).getChildren()) {
        if ("".equals(child.toRegexString())) {
          producesEmpty = true;
          continue;
        }
        apply(child, n);
      }
      if (producesEmpty) {
        if (n.getChildren().size() == 1) {
          n = new Repeat(n.getChildren().get(0), 0, 1);
        } else {
          n = new Repeat(n, 0, 1);
        }
      }

      addTo(n, parent);
      return n;
    } else {
      return super.apply(expr, parent);
    }
  }

  public static void main(String[] args) {
    Expr e = RegexParser.parse("(a||b)c(d||)");
    //e = RegexParser.parse("aa");
    System.out.println(e.toRegexString());
    System.out.println(e.print());
    e = new AltToRep().apply(e);
    System.out.println(e.toRegexString());
    System.out.println(e.print());
  }

}
