package fr.telecom_paristech.dbweb.regexrepair;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;

public class RegexParserTest {

  public void check(String str) {
    Expr e = RegexParser.parse(str);
    assertEquals(str, e.toRegexString());
  }

  @Test
  public void test() {
    check("a(d|e)*f[g]{2,3}");
    check(".|-");
    check("-|.");
    check("[{]");
    check("[^\\\"]");
    check("[^\"]");
    check("a?b*c+e{2,3}f{4,}g{5}");
    check("a|b");
  }

}
