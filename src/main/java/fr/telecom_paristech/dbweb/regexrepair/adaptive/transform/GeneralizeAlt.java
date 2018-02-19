package fr.telecom_paristech.dbweb.regexrepair.adaptive.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;

/**
 * Generalize multiple repetitions in an alternative
 * E.g. a{3}|a{7}|a{9,10} becomes a{3,10}
 * @author Thomas Rebele
 */
public class GeneralizeAlt extends CopyExpr {

  @Override
  protected Expr apply(Expr expr, AggExpr parent) {
    if (expr instanceof Alt) {

      // sort children by their string representation
      // (ignoring top level repetitions)
      TreeMap<String, List<Expr>> strToExpr = new TreeMap<>();
      List<String> order = new ArrayList<>();

      for (Expr c : ((Alt) expr).getChildren()) {
        Expr cc = c;
        while (cc instanceof Repeat) {
          cc = ((Repeat) cc).getChildren().get(0);
        }
        strToExpr.computeIfAbsent(cc.toRegexString(), k -> {
          order.add(k);
          return new ArrayList<>();
        }).add(c);
      }

      Alt a = new Alt();
      addTo(a, parent);
      for (String str : order) {
        List<Expr> cs = strToExpr.get(str);
        if (cs.size() > 2) {
          int min = Integer.MAX_VALUE;
          int max = 0;
          Expr unnested = null;
          for (Expr e : cs) {
            int localMin = 1;
            int localMax = 1;
            while (e instanceof Repeat) {
              localMin *= ((Repeat) e).getMin();
              localMax *= ((Repeat) e).getMax();
              e = ((Repeat) e).getChildren().get(0);
            }

            min = Math.min(min, localMin);
            max = Math.max(max, localMax);
            unnested = e;
          }
          cs = Arrays.asList(new Repeat(unnested, min, max));
        }
        for (Expr e : cs) {
          apply(e, a);
        }
      }

      return a;
    }
    else {
      return super.apply(expr, parent);
    }
  }

  public static void main(String[] args) {
    Expr e = RegexParser.parse("a{3}|a{7}|a{9,10}");
    System.out.println(e.toRegexString());
    System.out.println(e.print());

    e = new GeneralizeAlt().apply(e);
    System.out.println(e.toRegexString());
    System.out.println(e.print());
  }
}
