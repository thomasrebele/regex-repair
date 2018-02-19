package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;

/**
 * Base class for expressions.
 */
public abstract class Expr implements Cloneable {

  /** Store parent node */
  protected AggExpr parent = null;

  /** Transform string to regex, that matches exactly that string */
  public static Expr textExpr(String s) {
    if (s.length() == 1) {
      return new Text(s);
    }
    Conc c = new Conc();
    for (int i = 0; i < s.length(); i++) {
      c.addChild(new Text(s.substring(i, i + 1)));
    }
    return c;
  }

  /** Wrap expression in (...|) */
  public static Expr optional(Expr expr) {
    Alt alt = new Alt();
    alt.addChild(expr);
    alt.addChild(new Conc());
    return alt;
  }

  /** Whether this regex matches the string "" */
  public abstract boolean acceptsEmptyWord();

  /** Get height (number of steps to the deepest child). */
  public abstract int height();

  /** Get depth (number of steps to the root). */
  public int depth() {
    return 0;
  }

  /** Get parent node */
  public AggExpr getParent() {
    return parent;
  }

  /** Get root of regex tree */
  public Expr getRoot() {
    if (isRoot()) {
      return this;
    }
    return parent.getRoot();
  }

  /** Are we at the root of the tree ? */
  public boolean isRoot() {
    return parent == null || parent == this;
  }

  /** Get all leaf nodes of this subtree */
  public List<Expr> leaves() {
    List<Expr> l = new ArrayList<>();
    leaves(l);
    return l;
  }

  /** Add all leaf nodes to argument l */
  protected abstract void leaves(List<Expr> l);

  /** Get all ancestors of this node (including itself) */
  public List<Expr> getAncestors() {
    List<Expr> result = new ArrayList<>();
    Expr expr = this;
    do {
      result.add(expr);
      expr = expr.getParent();
    } while (expr != null && result.get(result.size() - 1) != expr);
    Collections.reverse(result);
    return result;
  }

  /** Get all ancestors of expression. (Convenience method) */
  public static List<Expr> getAncestors(Expr expr) {
    List<Expr> result = new ArrayList<>();
    if (expr != null) {
      result = expr.getAncestors();
    }
    return result;
  }

  @Override
  public Expr clone() {
    try {
      Expr expr = (Expr) super.clone();
      expr.parent = null;
      return expr;
    } catch (Exception e) {
      return null;
    }
  }

  /** Check whether child/parent relationship is correct */
  public void debug() {
    if (parent != null) {
      AggExpr p = parent;
      if (!p.getChildren().contains(this)) {
        throw new IllegalStateException("parent " + p + " doesn't accept its child " + this);
      }
    }
  }

  /** Interface for transforming regex tree */
  public interface Transform {

    public Expr apply(Expr originalNode, Expr transformed, Expr oldParent);
  }

  /**
   * Transform operator tree.
   * First transforms children, then applies fn to this node which children have been replaced
   * If you don't want to apply any specific operation just return the parameter
   * Parameters of fn: (original node, transformed node)
   */
  public abstract Expr transform(Transform fn, Expr oldParent);

  /** {@link #transform(Transform, Expr)} */
  public Expr transform(Transform fn) {
    return transform(fn, null);
  }

  /** {@link #transform(Transform, Expr)} */
  public Expr transform(Function<Expr, Expr> fn, Expr oldParent) {
    return transform((o, n, p) -> fn.apply(n), oldParent);
  }

  /** {@link #transform(Transform, Expr)} */
  public Expr transform(Function<Expr, Expr> fn) {
    return transform((o, n, p) -> fn.apply(n));
  }

  /** {@link #transform(Transform, Expr)} */
  public static List<Expr> transform(Transform fn, List<Expr> children, Expr oldParent) {
    return children.stream().map(c -> c.transform(fn, oldParent)).collect(Collectors.toList());
  }

  // functions related to string representations

  /** Get string representation of 'operator' of this node */
  public abstract String nodeText();

  @Override
  public String toString() {
    return getRoot().toRegexString(this);
  }

  /** Class name with id */
  protected String name() {
    return this.getClass().getName().replace(this.getClass().getPackage().getName() + ".", "").replace("Expr", "") + "@" + Tools.addr(this);
  }

  /** Helper class for a more general toString method */
  public static interface ToStringModifier {

    public String apply(Expr e, String s);

    public static String apply(ToStringModifier m, Expr e, String s) {
      return m == null ? s : m.apply(e, s);
    }
  }

  /** Constructs a string representation of the regex */
  public String toRegexString(ToStringModifier f) {
    throw new UnsupportedOperationException(this.getClass().toString());
  }

  /** Constructs a string representation of the regex */
  public final String toRegexString() {
    return toRegexString((Expr) null);
  }

  /** Constructs a string representation of the regex, highlighting the sub expression 'mark' */
  public final String toRegexString(Expr mark) {
    return toRegexString((e, s) -> mark == e ? mark(s) : s);
  }

  /** Generates Java code, that can be copy pasted to obtain this tree (for debugging) */
  public abstract String toJava();

  /** For debugging, print Expr as a tree */
  public String print() {
    StringBuilder sb = new StringBuilder();
    print(sb, "");
    return sb.toString();
  }

  /** Generate a string representation of the tree */
  protected void print(StringBuilder sb, String prefix) {
    printThis(sb, prefix);
  }

  /** Generate a string representation of the tree */
  protected void printThis(StringBuilder sb, String prefix) {
    sb.append(prefix);
    sb.append(name());
    sb.append(": ");
    sb.append(toRegexString());
    sb.append(" marked ");
    sb.append(getRoot() == null ? null : getRoot().toRegexString(this));
    sb.append("\n");
  }

  /** Add special parentheses */
  public static String mark(String str) {
    return "⟦" + str + "⟧";
  }

}