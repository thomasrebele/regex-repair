package fr.telecom_paristech.dbweb.regexrepair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.ExprPos;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;

// for testing
public class Base {

  public String regex;

  public Expr expr;

  public ExprPos start;

  public Base(Expr e) {
    regex = e.toRegexString();
    expr = e;
    start = ExprPos.first(expr);
  }

  public Base(String regex) {
    this(RegexParser.parse(regex));
  }

  /** Get subexpression of e, with has the regex str */
  private Expr pos(Expr e, String str) {
    if (Objects.equals(e.toRegexString(), str)) {
      return e;
    }
    if (e instanceof AggExpr) {
      for (Expr c : ((AggExpr) e).getChildren()) {
        Expr r = pos(c, str);
        if (r != null) {
          return r;
        }
      }
    }
    return null;
  }

  /** Get subexpression with has the regex str */
  public Expr pos(String str) {
    return pos(expr, str);
  }

  /**
   * Group a list to sublists, such that for each consecutive elements of a sublist 'combinable.apply(e1, e2) = true',
   * but for the last element e1 of sublist s1, and first element e2 of sublist s2, 'combinable.apply(e1, e2) = false'
   * 
   * Example: [1,2,3,4,7,8,9], combinable := 'e1 + 1 == e2', then we get sublists [1,2,3,4], [7,8,9]
   * @param l
   * @param combinable
   * @return
   */
  public static <T> List<List<T>> group(List<T> l, BiFunction<T, T, T> combinable) {
    List<List<T>> result = new ArrayList<>();
    T last = null;
    T lastT = null;
    List<T> group = new ArrayList<>();
    for (T t : l) {
      if (last == null) {
        last = t;
        lastT = t;
        continue;
      }
      T newT = combinable.apply(lastT, t);
      if (newT != null) {
        group.add(last);
        lastT = newT;
      } else {
        group.add(last);
        result.add(group);
        group = new ArrayList<>();
        lastT = t;
      }
      last = t;
    }
    if (last != null) {
      group.add(last);
    }
    if (group.size() > 0) {
      result.add(group);
    }
    return result;
  }

  /** Example how to use this class */
  public static void main(String[] args) {
    Base b = new Base("a(bc{4}){3}");
    System.out.println("start: " + b.start);
    ExprPos epa = b.start.advance(b.pos("a"));
    System.out.println(epa);
    ExprPos epb = b.start.advance(b.pos("b"));
    System.out.println(epb);
    ExprPos epbb = epb.advance(b.pos("b"));
    System.out.println(epbb);
    System.out.println(ExprPos.wasSingleStep(null, epb));
    System.out.println(ExprPos.wasSingleStep(null, epb, b.pos("(bc{4}){3}")));
    ExprPos epc = b.start.advance(b.pos("c"));
    System.out.println(ExprPos.wasSingleStep(null, epc, b.pos("(bc{4}){3}")));
    System.out.println(ExprPos.wasSingleStep(null, epc, b.pos("c{4}")));

    b = new Base("(a(b){2}c|def){3}");
    epbb = b.start.advance(b.pos("b")).advance(b.pos("b"));
    System.out.println(epbb);
    System.out.println(epbb.getCycle());

  }
}