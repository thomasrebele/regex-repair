package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.Objects;

/**
 * A quantnifier (Repetition of a subexpression), e.g. x* x+ x? x{2,3}
 */
public class Repeat extends AggExpr {

  /** A quantifier with an iteration counter */
  public static class RepeatCycle {

    public Repeat r;

    /** Cycle, start counting with 0 */
    public int c;

    public RepeatCycle(Repeat r, int c) {
      this.r = r;
      this.c = c;
    }

    @Override
    public String toString() {
      return r.toRegexString() + "@" + c;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (!(other instanceof RepeatCycle)) {
        return false;
      }
      RepeatCycle o = (RepeatCycle) other;
      return c == o.c && Objects.equals(r, o.r);
    }

    @Override
    public int hashCode() {
      // use toRegexString to make hashing independent of addr of r
      return 17 * (r == null ? 1 : Objects.hashCode(r.toRegexString(r))) + 31 * c;
    }
  }

  public Repeat() {
  }

  public Repeat(int min, int max) {
    this.min = min;
    this.max = max;
  }

  public Repeat(Expr expr, int min, int max) {
    this(min, max);
    if (expr != null) {
      addChild(expr);
    }
  }

  public RepeatCycle cycle(int cycle) {
    return new RepeatCycle(this, cycle);
  }

  @Override
  public boolean getCollapseSingleChild() {
    return false;
  }

  public final static int STAR = Integer.MAX_VALUE;

  private int min = 0;

  private int max = STAR;

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public void setMin(int val) {
    if (val < 0) {
      min = 0;
    } else {
      min = val;
    }
  }

  public void setMax(int val) {
    if (val < STAR) {
      max = val;
    } else {
      max = STAR;
    }
  }

  public void addToMin(int val) {
    setMin(val + min);
  }

  public void addToMax(int val) {
    if (max == STAR) {
      return;
    }
    long result = val;
    result += max;
    if (result > STAR) {
      result = STAR;
    }
    setMax((int) result);
  }

  @Override
  public boolean acceptsEmptyWord() {
    return min == 0 || children.get(0).acceptsEmptyWord();
  }

  @Override
  public int getPrecedence() {
    return 1;
  }

  @Override
  public String toRegexString(ToStringModifier m) {
    return this.toRegexString("", "", nodeText(), m);
  }

  @Override
  public String toJava() {

    return "new Repeat(" + (children.size() > 0 ? children.get(0).toJava() + ", " : "") + min + ", " + max + ")";
  }

  public Expr getChild() {
    return children.get(0);
  }

  @Override
  public String nodeText() {
    String suffix = "";
    int tmin = min < 0 ? 0 : min;
    int tmax = max < 0 ? 0 : max;
    if (tmax == STAR) {
      if (tmin == 0) {
        suffix = "*";
      } else if (tmin == 1) {
        suffix = "+";
      } else {
        suffix = "{" + tmin + ",}";
      }
    } else if (tmin == 0 && tmax == 1) {
      suffix = "?";
    } else {
      if (tmin == tmax) {
        suffix = "{" + tmin + "}";
      } else {
        suffix = "{" + tmin + "," + tmax + "}";
      }
    }
    return suffix;
  }

  @Override
  public Expr transform(Transform fn, Expr oldParent) {
    return fn.apply(this, new Repeat(children.get(0).transform(fn, oldParent), min, max), oldParent);
  }

}