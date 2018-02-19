package fr.telecom_paristech.dbweb.regexrepair;

import static fr.telecom_paristech.dbweb.regexrepair.RepairTests.check;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

public class QualityRepairTests {

  @Test
  public void test1() {
    check("\\d{2}", Arrays.asList("123"), Arrays.asList("", "1"));
  }

  @Test
  public void test2() {
    check("a(b|c|d)e", Arrays.asList("axb"), Arrays.asList("abxc"));
  }

  @Test
  public void test3() {
    check("(ab){2}", Arrays.asList("ba"), Arrays.asList("ab"));
  }

  @Test
  public void test4() {
    check("(emse|eater|Th){2}", Arrays.asList("Theaters"), Arrays.asList("s"));
  }

  @Test
  public void test5() {
    check("abc", Arrays.asList("aby", "axc"), Arrays.asList("ay", "abxc"));
  }

  @Test
  public void test6() {
    check("\\d{2}", Arrays.asList("Ab 1", "Abc 2"), Arrays.asList("Ab ", "Abc "));
  }

  @Test
  public void test7() {
    check("a{2}Ab{2}", Arrays.asList("aa_bb"), Arrays.asList("aAb", "ab", "aab", "abb", "_aAb", "_abb", "_aAbb"));
  }

  @Test
  public void test8() {
    check("a\\({0,1}\\d{3}\\){0,1}", Arrays.asList("a 123"), Arrays.asList("a( 123"));
  }

  @Test
  public void test9() {
    check("a(1|2)c", Arrays.asList("a3c"), Arrays.asList("ac"));
  }

  @Test
  public void test10() {
    check("aaa_aa", Arrays.asList("aaa_b", "axa_aa"), Arrays.asList("aaa_baa", "aaa_aab"));
  }

  @After
  public void sumarize() {
    RepairTests.printResults();
  }

}
