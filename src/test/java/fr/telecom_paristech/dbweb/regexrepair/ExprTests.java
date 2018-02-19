package fr.telecom_paristech.dbweb.regexrepair;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;


public class ExprTests {

  public static void checkStr(String exp, String str) {
    Expr e = RegexParser.parse(str);
    assertEquals(exp, e.toRegexString());
  }

  public static void checkStr(String str) {
    checkStr(str, str);
  }

  @Test
  public void testToRegexString() {
    checkStr("ab");
    checkStr("a|b");
    checkStr("a*");
    checkStr("a?");
    checkStr("a+");
    checkStr("a{2,3}");
    checkStr("\\d");
    checkStr("a(a|b)");

    checkStr("a", "((a))");
    checkStr("a|b", "(((a)|(b)))");
    checkStr("a(a*|bcd{2,3})", "a(a*|b(cd{2,3}))");
    checkStr("a|b|c|d", "a|b|(c|d)");
  }
}
