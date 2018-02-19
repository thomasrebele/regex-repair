package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.Arrays;
import java.util.Collection;

/**
 * Alternative x|y|z
 */
public class Alt extends AggExpr {

  public Alt() {
  }

  public Alt(Expr... children) {
    this.addChildren(Arrays.asList(children));
  }

  public Alt(Collection<Expr> children) {
    this.addChildren(children);
  }

  @Override
  public int getPrecedence() {
    return 3;
  };

  @Override
  public boolean acceptsEmptyWord() {
    boolean result = children.size() == 0;
    for (Expr child : children) {
      result |= child.acceptsEmptyWord();
      if (result) {
        return true;
      }
    }
    return result;
  }

  @Override
  public String toRegexString(ToStringModifier m) {
    return toRegexString("", "|", "", m);
  }

  @Override
  public String nodeText() {
    return "|";
  }

  @Override
  public Expr transform(Transform fn, Expr oldParent) {
    return fn.apply(this, new Alt(transform(fn, children, this)), oldParent);
  }

}