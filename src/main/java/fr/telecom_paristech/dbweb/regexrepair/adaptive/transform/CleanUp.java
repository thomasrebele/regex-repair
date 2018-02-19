package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Text;

/**
 * Cleans up the regex tree without changing the semantics of the regex.
 * Does the following:
 * - combine concatenation within concatenation to one level
 * - same for alt
 * - remove {1} quantifiers
 */
public class CleanUp extends CopyExpr {
  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (expr instanceof AggExpr ) {
      AggExpr ae = (AggExpr)expr;
      if (ae.getCollapseSingleChild() && ae.getChildren().size() == 1) {
        return apply(ae.getChildren().get(0), parent);
      }
    }

    if (expr instanceof Conc && parent instanceof Conc) {
      for (Expr child : ((Conc) expr).getChildren()) {
        if (child.toRegexString().length() == 0) {
          continue;
        }
        apply(child, parent);
      }
      return parent;
    } else if (expr instanceof Alt) {
      Alt copy = (Alt) copy(expr);
      applyOnAlt((Alt) expr, copy, new HashSet<>());
      if (Arrays.asList("()", "").contains(copy.toRegexString())) {
        return parent == null ? new Conc() : parent;
      } else {
        return addTo(copy, parent);
      }
    } else if (expr instanceof Repeat) {
      Repeat r = (Repeat) expr;
      if (r.getChildren().size() > 0) {
        Repeat rCopy = (Repeat) copy(expr);
        apply(r.getChild(), rCopy);
        if (rCopy.getChildren().size() == 0) {
          return parent;
        } else if (rCopy.getMin() == 1 && rCopy.getMax() == 1) {
          return addTo(rCopy.getChild(), parent);
        }
        else {
          return addTo(rCopy, parent);
        }
      }

      return parent;
    }
    return super.apply(expr, parent);
  }

  protected void applyOnAlt(Alt expr, Alt copy, Set<String> children) {
    for (Expr child : expr.getChildren()) {
      child = apply(child, null);
      if (child instanceof Alt) {
        applyOnAlt((Alt) child, copy, children);
      } else {
        String regex = child.toRegexString();
        if (!children.contains(regex) && !children.contains("(" + regex + ")")) {
          copy.addChild(child);
          children.add(regex);
        }
      }
    }
  }

  public static void main(String[] args) {
    Expr e;
    e = RegexParser.parse("(|1)((||3)|)\\d((|(|\\)) )|0)");
    e = RegexParser.parse("(|1)((||3)|)\\d|((|(|\\)|))|0)");
    e = new EmbedInConc().apply(RegexParser.parse("((.|-)|)"));
    e = RegexParser.parse("\\d()(\\d{1}|)()-\\d(|-ABCD)");
    //e = RegexParser.parse("(|(|\\)) )");
    //e = RegexParser.parse("(abe)def()ab(ab)");
    e = new Conc(new Alt(new Conc(new Alt(new Conc(), new Conc(new Text(" ")))), new Conc()));

    System.out.println(e.toRegexString());
    System.out.println(e.print());
    Expr ne = new CleanUp().apply(e);
    System.out.println(ne.toRegexString());
    System.out.println(ne.print());

    System.out.println("old e: \n" + e.print());
  }

}
