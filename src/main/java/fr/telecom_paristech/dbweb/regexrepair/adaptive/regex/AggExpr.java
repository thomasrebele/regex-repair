package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Base class for aggregated expression, i.e. expressions that have child expressions in some form.
 * (E.g. concatenation, alternative)
 */
public abstract class AggExpr extends Expr {

  protected List<Expr> children = new ArrayList<>();

  public List<Expr> getChildren() {
    return Collections.unmodifiableList(children);
  }

  /** Add expression as child */
  public void addChild(Expr expr) {
    this.children.add(expr);
    expr.parent = this;
  }

  /** Add all expressions as children */
  public void addChildren(Collection<Expr> children) {
    this.children.addAll(children);
    for (Expr e : children) {
      e.parent = this;
    }
  }

  /** Remove current children, add argument as new children */
  public void replaceChildren(List<Expr> children) {
    for (Expr e : children) {
      e.parent = null;
    }
    this.children = new ArrayList<>(children);
    for (Expr e : children) {
      e.parent = this;
    }
    this.debug();
  }

  /** Replace one child by a new expression */
  public void substituteChild(Expr oldChild, Expr newChild) {
    for (int i = 0; i < children.size(); i++) {
      Expr child = children.get(i);
      if (child == oldChild) {
        newChild.parent = this;
        children.set(i, newChild);
        child.parent = null;
      }
    }
  }

  /** Remove last child */
  public void removeLastChild() {
    if (children.size() > 0) {
      children.remove(children.size() - 1).parent = null;
    }
  }

  /** Remove all children */
  public void clearChildren() {
    for (Expr e : children) {
      e.parent = null;
    }
    this.children.clear();
  }

  @Override
  public Expr clone() {
    try {
      Expr expr = super.clone();
      ((AggExpr) expr).children = new ArrayList<>();
      return expr;
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  protected void leaves(List<Expr> l) {
    for (Expr c : children) {
      c.leaves(l);
    }
  }

  /** If a concatenation/alternative node has only one child, we don't need the concatenation or alternative node */
  public boolean getCollapseSingleChild() {
    return true;
  }

  @Override
  public void debug() {
    for (Expr child : children) {
      if (child.parent != this) {
        throw new IllegalStateException("child " + child + " of tree " + this + " thinks it belongs to " + child.parent);
      }
      child.debug();
    }
    super.debug();
  }

  // functions related to string representations

  @Override
  protected void print(StringBuilder sb, String prefix) {
    printThis(sb, prefix);
    for (Expr child : children) {
      child.print(sb, prefix + "\t");
    }
  }

  @Override
  public String toJava() {
    String arg = children.stream().map(child -> child.toJava()).collect(Collectors.joining(", "));
    return "new " + name().replaceAll("@.*", "") + "(" + arg + ")";
  }

  /** Precedence of this type of operator. Quantifiers have higher precedence than concatenations, which have higher precedence than alternatives.
   * ab{2}c|de{3} is interpreted as (a(b{2})c)|(d(e{3})) */
  public int getPrecedence() {
    throw new Error("implement this method in the subclasses");
  };

  /**
   * Helper method for child classes. Initialize the string builder with prefix, 
   * initialize children (and add the delimiter and parentheses if necessary),
   * and finish by adding the suffix.
   * @param sb
   * @param prefix
   * @param delimiter
   * @param suffix
   */
  protected String toRegexString(String prefix, String delimiter, String suffix, ToStringModifier m) {
    StringBuilder sb = new StringBuilder();

    sb.append(prefix);
    for (int i = 0; i < children.size(); i++) {
      Expr child = children.get(i);
      if (i != 0) {
        sb.append(delimiter);
      }
      // check whether child needs parentheses
      boolean needsParentheses = false;
      String childStr = child.toRegexString(m);
      if (child instanceof AggExpr) {
        AggExpr achild = (AggExpr) child;
        int childPrecedence = achild.getPrecedence();
        if (childPrecedence > this.getPrecedence()) {
          needsParentheses = true;
        }
        if (child instanceof Repeat && this instanceof Repeat) {
          needsParentheses = true;
        }
        // hide parentheses for single character subexpressions (Q&D)
        if (childStr.length() > 0) {
          if (childStr.length() == 1 && childStr.charAt(0) != '|') {
            needsParentheses = false;
          }
          if (childStr.length() == 2 && childStr.charAt(0) == '\\') {
            needsParentheses = false;
          }
          // avoid error for Pattern.compile("a+*")
          if (("+".equals(suffix) || "*".equals(suffix)) && "+".equals(childStr.substring(childStr.length() - 1))) {
            needsParentheses = true;
          }
        }
      }

      child.toRegexString(m);
      if (needsParentheses) {
        sb.append("(");
      }
      sb.append(childStr);
      if (needsParentheses) {
        sb.append(")");
      }
    }
    sb.append(suffix);

    return ToStringModifier.apply(m, this, sb.toString());
  }

  @Override
  public int height() {
    return 1 + children.stream().mapToInt(e -> e.height()).max().orElse(0);
  }

  @Override
  public int depth() {
    return parent == null ? 0 : parent.depth() + 1;
  }
}
