package fr.telecom_paristech.dbweb.regexrepair;

import static org.junit.Assert.assertEquals;

import java.util.function.Function;

import org.junit.Test;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.AltToRep;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.FoldRepeat;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.SimplifyRepeat;

public class TransformTest {

  public void base(String expected, String regex, Function<Expr, Expr> f) {
    Expr e = RegexParser.parse(regex);
    e = f.apply(e);
    System.out.println("----------");
    System.out.println("regex:       " + regex);
    System.out.println("transformed: " + e.toRegexString());
    assertEquals(expected, e.toRegexString());
  }

  @Test
  public void test() {
    Expr e = RegexParser.parse("(| )a{1}");
    e = new SimplifyRepeat().apply(e);
    e.debug();
  }

  @Test
  public void test2() {
    base("a? b{12} (b{2,3}){4}", "((a)?)? (b{3}){4} (b{2,3}){4}", e -> new SimplifyRepeat().apply(e));
  }

  @Test
  public void test3() {
    base("(abc(def)*)? (ab|def)?", "(|abc(def)*) (ab||def)", e -> new AltToRep().apply(e));
  }

  @Test
  public void test4() {
    base("a{4,}", "aaaaa*", e -> new FoldRepeat().apply(e));
    base("a+", "aa?a?a*", e -> new FoldRepeat().apply(e));
    base("a{9}", "a{2}a{4}aaa", e -> new FoldRepeat().apply(e));

    base("\\d{4,}", "\\d\\d\\d\\d\\d*", e -> new FoldRepeat().apply(e));
    base("\\d+", "\\d\\d?\\d?\\d*", e -> new FoldRepeat().apply(e));

    base("a{5}", "a{2}a{3}", e -> new FoldRepeat().apply(e));
    base("a{6}", "a{2}aa{3}", e -> new FoldRepeat().apply(e));
    base("a+", "a{1,2}a*", e -> new FoldRepeat().apply(e));
    base("(a|b)+", "(a|b){1,2}(a|b)*", e -> new FoldRepeat().apply(e));
    base("(a|b)+", "(a|b){1,2}(a|b){0,2}(a|b)?(a|b)?(a|b){0,2}(a|b)?(a|b)*", e -> new FoldRepeat().apply(e));
  }
}
