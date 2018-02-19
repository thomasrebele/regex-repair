package fr.telecom_paristech.dbweb.regexrepair;

// commented because of changing interface between matcher and repairer (removal of ExprPos)
/*public class MatcherTest {


  java.util.List<Expr> match(String regex, String word) {
    return PartialPattern.match(regex, word);
  }

  @Test
  public void test1() {
    List<CharMatch> r = match("(a\\d{2}){2}", "aa12a12");
    assertEquals(6, r.size());

    r = match("(abc)*", "bb");
    assertEquals(2, r.size());

    r = match("(abc)*", "abbc");
    assertEquals(4, r.size());

    r = match("(abc)*", "bcab");
    assertEquals(4, r.size());

    r = match("(abc)*", "abcbcd");
    assertEquals(5, r.size());

    r = match("(abcd)*", "abcbcd");
    assertEquals(6, r.size());

    r = match("(abcde)*", "abcdbcde");
    assertEquals(8, r.size());

  }

  @Test
  public void test2() {
    List<CharMatch> r = match("\\d{2}", "1234");
    assertEquals(2, r.size());

    r = match("(|Ab )\\d(\\d{1}|)", "Abc 2");
    assertEquals(4, r.size());

    r = match("\\d{2}", "Ab 2");
    assertEquals(1, r.size());
  }

  @Test
  public void test3() {
    List<CharMatch> r = match("\\d{2}x\\d{2}", "x12-34");
    assertEquals(4, r.size());
  }

  @Test
  public void test4() {
    List<CharMatch> r = match("\\d{2}a\\d{2}b\\d{3}", "a12x34x567");
    assertEquals(7, r.size());
  }

  @Test
  public void test5() {
    List<CharMatch> r = match("1\\d{3}", "1234");
    assertEquals(4, r.size());
  }

  @Test
  public void test6() {
    String str = "1a2b3456";
    List<CharMatch> r = match("\\d{4}", str);
    System.out.println(r);
    assertEquals(4, r.size());
    assertEquals(str.indexOf("3"), r.get(0).substr.start);
  }

  @Test
  public void test7() {
    String str = "12a3b45";
    List<CharMatch> r = match("\\d{4}", str);
    System.out.println(r);
    assertEquals(4, r.size());
    assertEquals(str.indexOf("4"), r.get(2).substr.start);
  }

  @Test
  public void test8() {
    String str = "AuA 611";
    List<CharMatch> r = match("[A]{1,3}(( ))\\d{3}", str);
    System.out.println(r);
    assertEquals(6, r.size());
    assertEquals(str.indexOf(" "), r.get(2).substr.start);
  }

  @Test
  public void test9() {
    List<CharMatch> r = match("(Aa)?", "aaa");
    assertEquals(1, r.size());
  }

}*/
