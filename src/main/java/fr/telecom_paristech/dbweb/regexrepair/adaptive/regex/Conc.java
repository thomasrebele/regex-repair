package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.Arrays;
import java.util.List;

/**
 * Concatenation xyz
 */
public class Conc extends AggExpr {

  public Conc() {

  }

  public Conc(Expr... children) {
    this.addChildren(Arrays.asList(children));
  }

  public Conc(List<Expr> children) {
    this.addChildren(children);
  }

  @Override
  public int getPrecedence() {
    return 2;
  }

  @Override
  public boolean acceptsEmptyWord() {
    boolean result = children.size() == 0;
    for (Expr child : children) {
      result &= child.acceptsEmptyWord();
    }
    return result;
  }

  @Override
  public String toRegexString(ToStringModifier m) {
    return toRegexString("", "", "", m);
  }

  @Override
  public String nodeText() {
    return "&";
  }

  @Override
  public Expr transform(Transform fn, Expr oldParent) {
    return fn.apply(this, new Conc(transform(fn, children, this)), oldParent);
  }
}