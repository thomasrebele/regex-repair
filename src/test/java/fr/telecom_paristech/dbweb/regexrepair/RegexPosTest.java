package fr.telecom_paristech.dbweb.regexrepair;

import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.ExprPos;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.EmbedInConc;

public class RegexPosTest {

  //  @Test
  //  public void getStrictIndexTest() {
  //    Base base = new Base("(a|b|c)(d|e|f)");
  //    List<Expr> list = ((Conc) base.expr).getChildren();
  //
  //    int idx = 0;
  //    /*idx = StdExprPos.getStrictIndex(list, new TextSpan(base.regex, 0, 0));
  //    assertEquals(-1, idx);*/
  //
  //    idx = list.indexOf(base.pos("b"));
  //    assertEquals(0, idx);
  //
  //    idx = list.indexOf(base.pos("e"));
  //    assertEquals(1, idx);
  //
  //    idx = list.indexOf(ExprPos.last(base.expr));
  //    assertEquals(list.size(), idx);
  //  }

  @Test
  public void basicTest() {
    Base base = new Base("a");
    Expr a = base.pos("a");
    ExprPos pos = base.start.advance(a);
    ExprPos pos2 = base.start.advance(a);
    Assert.assertEquals(a, pos.getExpr());
    Assert.assertEquals(pos, pos2);
    Assert.assertEquals(pos.hashCode(), pos2.hashCode());

    Assert.assertNull(pos.advance(base.expr));
    Assert.assertNull(pos.advance(a));
    //Assert.assertNull(base.expr.advance(pos, new TextSpan(base.regex, 1, 1)));
  }

  @Test
  public void concTest() {
    Base base = new Base("abcde");
    Expr a = base.pos("a");
    Expr c = base.pos("c");
    ExprPos pos = base.start.advance(a);
    Assert.assertEquals(a, pos.getExpr());

    Assert.assertNull(pos.advance(base.expr));
    Assert.assertNull(pos.advance(a));

    ExprPos pos2 = pos.advance(c);
    Assert.assertEquals(c, pos2.getExpr());
    Assert.assertNull(pos2.advance(c));
  }

  @Test
  public void altTest() {
    Base base = new Base("(a|b|c|d|e)");
    Expr a = base.pos("a");
    Expr c = base.pos("c");
    ExprPos pos = base.start.advance(a);
    Assert.assertEquals(a, pos.getExpr());

    Assert.assertNull(pos.advance(base.expr));
    Assert.assertNull(pos.advance(a));
    Assert.assertNull(pos.advance(c));

    ExprPos pos2 = base.start.advance(c);
    Assert.assertEquals(c, pos2.getExpr());
  }

  @Test
  public void altConcTest() {
    Base base = new Base("(abc|def)");
    Expr a = base.pos("a");
    Expr c = base.pos("c");
    Expr e = base.pos("e");
    ExprPos pos = base.start.advance(a);
    Assert.assertEquals(a, pos.getExpr());

    Assert.assertNull(pos.advance(base.expr));
    Assert.assertNull(pos.advance(a));
    Assert.assertEquals(c, pos.advance(c).getExpr());

    ExprPos pos2 = base.start.advance(c);
    Assert.assertEquals(c, pos2.getExpr());
  }

  @Test
  public void concAltTest() {
    Base base = new Base("(a|b|c)(d|e|f)");
    Expr b = base.pos("b");
    Expr c = base.pos("c");
    Expr e = base.pos("e");
    ExprPos pos = base.start.advance(b);
    Assert.assertEquals(b, pos.getExpr());

    Assert.assertNull(pos.advance(base.expr));
    Assert.assertNull(pos.advance(b));
    Assert.assertNull(pos.advance(c));

    ExprPos pos2 = pos.advance(e);
    Assert.assertEquals(e, pos2.getExpr());
  }

  @Test
  public void repTest() {
    Base base = new Base(new EmbedInConc().apply(RegexParser.parse("a{2}")));
    Expr a = base.pos("a");
    ExprPos pos = base.start;

    pos = pos.advance(a);
    Assert.assertEquals(a, pos.getExpr());
    Assert.assertEquals(0, pos.getRepeatCycles().get(0).c);

    pos = pos.advance(a);
    Assert.assertEquals(a, pos.getExpr());
    Assert.assertEquals(1, pos.getRepeatCycles().get(0).c);

    pos = pos.advance(a);
    Assert.assertFalse(pos != null);
  }

  @Test
  public void nestedRepTest() {
    Base base = new Base("(a{2}x){2}");
    Expr a = base.pos("a");
    ExprPos pos = base.start;

    pos = pos.advance(a);
    Assert.assertEquals(a, pos.getExpr());
    Assert.assertEquals(0, pos.getRepeatCycles().get(0).c);

    pos = pos.advance(a);
    Assert.assertEquals(a, pos.getExpr());

    pos = pos.advance(a);
    Assert.assertTrue(pos != null);
    Assert.assertEquals(1, pos.getRepeatCycles().get(0).c);

    pos = pos.advance(a);
    Assert.assertTrue(pos != null);

    pos = pos.advance(a);
    Assert.assertNull(pos);


  }

  @Test
  public void test3() {
    Base base = new Base("(b(c|d){2}){2}");
    ExprPos pos = base.start;

    pos = pos.advance(base.pos("c"));
    System.out.println(pos);
    pos = pos.advance(base.pos("d"));
    System.out.println(pos);
  }

  @Test
  public void test4() {
    Base base = new Base("[a]{1,3}(( ))\\d{3}");
    Expr a = base.pos("[a]");

    ExprPos pos = base.start;
    System.out.println("pos " + pos + " span " + a);
    pos = pos.advance(a);
    //assertEquals("[a]{1,3}", pos.getExpr().toRegexString());
    assertEquals(0, pos.getRepeatCycles().get(0).c);
    pos = pos.advance(a);
    //assertEquals("[a]{1,3}", pos.getExpr().toRegexString());
    assertEquals(1, pos.getRepeatCycles().get(0).c);
    System.out.println("pos " + pos + " span " + a);
  }

  @Test
  public void test5() {
    Base base = new Base(new EmbedInConc().apply(RegexParser.parse("a{2}")));
    Expr a = base.pos("a");

    ExprPos pos = base.start;

    System.out.println("------");
    System.out.println(pos);
    pos = pos.advance(a);
    System.out.println(pos);
    //assertEquals("a{2}", pos.getExpr().toRegexString());
    pos = pos.advance(a);
    System.out.println(pos);
    pos = pos.advance(a);
    System.out.println(pos);
    org.junit.Assert.assertTrue(pos == null);
    System.out.println("------");
  }


  @Test
  public void test6a() {
    Base base = new Base(RegexParser.parse("abc(def){3}ghi"));
    String letters/**/ = "acdfddefg";
    String singleSteps = "ynynynyyy";
    checkWasSingleStep(base, letters, singleSteps);
  }

  @Test
  public void test6b() {
    Base base = new Base("a(bc|xy)");
    String letters/**/ = "x";
    String singleSteps = "n";
    checkWasSingleStep(base, letters, singleSteps);
  }

  @Test
  public void test6c() {
    Base base = new Base("a(bc|){3}d");
    String letters/**/ = "d";
    String singleSteps = "n";
    checkWasSingleStep(base, letters, singleSteps);
  }

  @Test
  public void test6d() {
    Base base = new Base("a(bc|){3}d");
    String letters/**/ = "bcd";
    String singleSteps = "nyn";
    checkWasSingleStep(base, letters, singleSteps);
  }

  @Test
  public void test6e() {
    Base base = new Base(RegexParser.parse("a+be+"));

    String letters/**/ = "be";
    String singleSteps = "nyy";
    checkWasSingleStep(base, letters, singleSteps);
  }

  @Test
  public void test6f() {
    Base base = new Base(RegexParser.parse("(a|b)"));
    String letters/**/ = "";
    String singleSteps = "n";
    checkWasSingleStep(base, letters, singleSteps);
  }

  public ExprPos checkWasSingleStep(Base base, String letters, String singleSteps) {
    return checkWasSingleStep(base, letters, singleSteps, base.expr.getRoot());
  }

  public ExprPos checkWasSingleStep(Base base, String letters, String singleSteps, Expr withinExpr) {
    ExprPos pos = base.start;

    int i = 0;
    for (i = 0; i < letters.length(); i++) {
      ExprPos last = pos;
      Expr dst = base.pos("" + letters.charAt(i));
      pos = pos.advance(dst);
      boolean expSingleStep = singleSteps.charAt(i) == 'y';

      boolean wasSingleStep = ExprPos.wasSingleStep(last, pos, withinExpr);
      if (expSingleStep != wasSingleStep) {
        Assert
        .fail("expected " + expSingleStep + ", but advancing " + last + " to " + dst + " resulted in " + pos + ", single step " + wasSingleStep);
      }
    }
    if (i < singleSteps.length()) {
      boolean expSingleStep = singleSteps.charAt(i) == 'y';
      boolean wasSingleStep = ExprPos.wasSingleStep(pos, ExprPos.last(base.expr), withinExpr);
      if (expSingleStep != wasSingleStep) {
        Assert.fail("expected " + expSingleStep + ", but advancing " + pos + " to end resulted in " + pos + ", single step " + wasSingleStep);
      }
    }
    return pos;
  }
}
