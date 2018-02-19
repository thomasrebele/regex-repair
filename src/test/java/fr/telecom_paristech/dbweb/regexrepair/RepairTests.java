package fr.telecom_paristech.dbweb.regexrepair;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.google.re2j.Pattern;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.AdaptiveRepairer;
import fr.telecom_paristech.dbweb.regexrepair.data.Table;
import fr.telecom_paristech.dbweb.regexrepair.iface.RegexRepairer;

public class RepairTests {

  static class TestCase {

    String regex;

    List<String> toadd;

    List<String> negatives;

    String repaired;
  }

  private static List<TestCase> done = new ArrayList<>();

  static RegexRepairer repairer = new AdaptiveRepairer();

  public static void check(String regex, String toadd) {
    check(regex, Arrays.asList(toadd), null);
  }

  public static void check(String regex, List<String> toadd) {
    check(regex, toadd, null);
  }

  public static void check(String regex, List<String> toadd, List<String> negatives) {
    System.out.println("------------------------------");
    String repaired = repair(regex, toadd);
    Pattern p = Pattern.compile(repaired);
    System.out.println("regex: " + regex);
    System.out.println("toadd: " + toadd);
    System.out.println("repaired: " + repaired);

    TestCase tc = new TestCase();
    tc.regex = regex;
    tc.toadd = toadd;
    tc.negatives = negatives;
    tc.repaired = repaired;
    done.add(tc);

    for (String str : toadd) {
      if (!p.matcher(str).matches()) {
        System.out.println("repaired regex doesn't match!");
        Assert.fail("repaired regex '" + repaired + "' doesn't match '" + str + "', original regex '" + regex + "'");
      }
    }
    if (negatives != null) {
      for (String negative : negatives) {
        if (p.matcher(negative).matches()) {
          String str = "shouldn't match '" + negative + "'";
          str += "\nregex: " + regex;
          str += "\ntoadd: " + toadd;
          str += "\nrepaired: " + repaired;
          System.out.println(str);
          Assert.fail(str);
        }
      }
    }
    System.out.println();
  }

  public static String repair(String regex, List<String> toadd) {
    return repairer.repair(regex, toadd, null);
  }

  public static void printResults() {
    Table<String> t = new Table<>(4, done.size(), "", null);
    for (int i = 0; i < done.size(); i++) {
      TestCase tc = done.get(i);
      t.set(0, i, tc.regex);
      t.set(1, i, tc.toadd == null ? "" : tc.toadd.toString());
      t.set(2, i, tc.negatives == null ? "" : tc.negatives.toString());
      t.set(3, i, tc.repaired);
    }
    System.out.println(t);
    System.out.println();
  }

  @After
  public void sumarize() {
    printResults();
  }

  @Test
  public void testUrls() {
    check("(http.//)?(\\w+.)\\w+", Arrays.asList("ftp://a", "https://b", "https://a"));
  }

  @Test
  public void c1() {
    check("Ab", Arrays.asList("Aba"));
    check("ab", Arrays.asList("cb"));
    check("abc", "ab");
    check("abcdef", Arrays.asList("abef"));
  }

  @Test
  public void c2() {
    check("abc", Arrays.asList("xyc", "axy"));
    check("abc", Arrays.asList("axc", "ayc"));
    check("abc", Arrays.asList("axc", "aby"));
    check("abcd", Arrays.asList("abxy", "axyd"));
  }

  @Test
  public void c3() {
    check("abc", Arrays.asList("axbc", "aybc", "a12c"));
  }

  @Test
  public void r1() {
    check("a{2}", Arrays.asList("a"));
    check("a{2}", Arrays.asList("aba"));

    check("\\d{2}", Arrays.asList("123"));
    check("\\d{3}", Arrays.asList("(234"));
    check("\\d{4}", Arrays.asList("764a"));
  }

  @Test
  public void rc1() {
    check("(ab){2}", Arrays.asList("ba"));

    check("(ab){2}", Arrays.asList("aba"));
    check("(ab){2}", Arrays.asList("a"));
    check("(ab){2}", Arrays.asList("bab"));
    check("(ab){2}", Arrays.asList("b"));

    check("(abc){3}", Arrays.asList("abcac-abc"));

  }

  @Test
  public void sc1() {
    check("(abc)*", "abbc");
    check("(abc)*", "abxbc");

    check("(abcd)*", Arrays.asList("aacccca"));
  }

  @Test
  public void test2() {

    check("[ ]{1,2}(1\\d{2}|)", " A1");
    check(" {1,2}", "  A");

    check("(|1)(3)\\d(|\\(|\\))", Arrays.asList("11-"));
    check("k(a|b)", Arrays.asList("kca"));

    check("a(bc|)d", Arrays.asList("b0c"));
    check("(ab*c)+", "aaca");
    check("(a+b)+", "baba");

    check("(emse|eater|Th){2}", Arrays.asList("Theaters"));

  }

  @Test
  public void test3() {

    check("(|a )\\d", Arrays.asList("ab 1"));
    check("\\d{2}", Arrays.asList("Ab 1", "Abc 2"));
    check("\\d{1} ", Arrays.asList("11 ", "11-", "11", "34) ", "40"));

    check("\\d{2}", Arrays.asList("1b23", "1b23"));

    check("(\\da){2}", Arrays.asList("1"));
    check("(\\d{2}){2}", Arrays.asList("1"));

    check("\\d{2}a\\d{2}b\\d{3}", Arrays.asList("a12x34x567"));
  }

  @Test
  public void test3a() {
    check("\\d{3} \\d{3}-\\d{4}", Arrays.asList(" 123-456-abc"));
    check("\\d{2}x\\d{2}", Arrays.asList("x1234"));

    check("34\\d{13}", Arrays.asList("370000000000002"));
    check("\\d{1}-\\d-\\d{1}", Arrays.asList("(2)2-58"));

    check("\\d{4}-\\d{3}", Arrays.asList("1234 (456)"));
    check("\\d{2}-\\d{2}", "1-2-A");
    check("\\d{3}-\\d{4}", "1-123-ABCD");

  }

  @Test
  public void test3b() {
    check("(\\d{2}){2}", Arrays.asList("123"));
    check("(\\d{2}a){2}", Arrays.asList("a123"));
    check("(\\d{2}a){2}", Arrays.asList("123"));
    check("(\\d{2}a){2}", Arrays.asList("a12"));

  }

  @Test
  public void test3c() {
    check("[A-Za-z]{3} 1", Arrays.asList("AB    1"));
    check("(a+ ){2}b", Arrays.asList("aa aa aa b"));
  }

  @Test
  public void test4() {
    check("a(|a{5}|a{7}|a{13})", Arrays.asList("aaaaaaaaaaaaaaaaaaa", "aaaaaaaaaaaaaaaaaaaaaaa"));
  }

  @Test
  public void test5() {
    check("h?", Arrays.asList("hw"));
    check("(\\d{2})?", Arrays.asList("2.0"));
    check("(.\\d{2})?", Arrays.asList(" 2.0"));

    check("a+", Arrays.asList("aba"));
    check("\\s+\\w+", Arrays.asList(" a 1.2"));
    check("\\w+.\\w+", Arrays.asList(".c#e#"));
    check("a+be+", Arrays.asList("baced"));
    check(".(-)+e", Arrays.asList(";-;-;e"));
    check("(a|i)+", Arrays.asList("#a?i"));

    check("(|1)(3)\\d(|\\(|\\))", Arrays.asList("11-"));
    check("(a|b)((c|d)(e|f))*", "b-c");

    check("abc", " *");
    check("(abc)*", "abbc");

    check("(abcde)*", "abcdbcde");

    check("(ab){3}", Arrays.asList("aXaYab"));
    check("(ab)*", Arrays.asList("aXaYab"));

    check("(ab)*", Arrays.asList("aXaYb"));
    check("(/a+)*", Arrays.asList("/A/0i/a"));

    check("[A]{1,3}", Arrays.asList("AuA"));

    check("( [A-Z][A-Za-z]){1,2}", " Aaaaaaa A");
    check("[A-Z][A-Za-z{\'}]*,( [A-Z][A-Za-z]){1,2}", "Aaaaa, Aaaaaaa A");
    check("a{4}[|-]a{2}", Arrays.asList("aaa-aaaaa"));
  }

  @Test
  public void test6() {
    // DDRB
    check("[A]{1,3}(( ))\\d{3}", Arrays.asList("AuA 611"));
    check("(.*a){2}", Arrays.asList("a!"));
  }

  @Test
  public void test7() {
    // step
    check("/b/", Arrays.asList("/ab/", "/c/"));
    check("abc", Arrays.asList("c", "b1c"));

    check("abcd", Arrays.asList("b0c", "ad"));
    check("abcd", Arrays.asList("b0c", "ad", "abcxd"));
  }

  @Test
  public void test8() {
    Assert.assertThat(repair("(.|-)", Arrays.asList("")), anyOf(is(".|-|"), is("|.|-"), is("[.\\-]?"), is("[\\-.]?"), is("(.|-)?"), is(".?")));
    Assert.assertThat(repair("(-|.)", Arrays.asList("")), anyOf(is("-|.|"), is("|-|."), is("[.\\-]?"), is("[\\-.]?"), is("(-|.)?"), is(".?")));
  }

  @Test
  public void combination() {
    // don't change order within blocks

    // block 1
    check(" {1,2}", "  A");
    check("[ ]{1,2}(1\\d{2}|)", " A1");

    // block 2
    check("[ ]{1,2}(1\\d{2}|)", " A1");
    check(" {1,2}", "  A");
  }

}
