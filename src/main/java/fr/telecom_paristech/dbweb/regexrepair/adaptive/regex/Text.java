package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;

/**
 * Just text (and nothing else)
 */
public class Text extends Expr {

  public String text;

  public Text(String string) {
    if (string.length() != 1 && string.length() != 2) {
      throw new UnsupportedOperationException("cannot use '" + string + "' as text, length " + string.length());
    }
    text = escape(string);
  }

  public Text(String string, boolean escape) {
    text = escape ? escape(string) : string;
  }

  /**
   * Escape regex related meta characters with \ (backslash)
   * @param str
   * @return
   */
  public static String escape(String str) {
    StringBuilder sb = new StringBuilder();
    String meta = "<([{\\^-=$!|]})?*+.>";
    meta = "()[]{}\\|?+.$*^";
    for (int i = 0; i < str.length(); i++) {
      if (meta.indexOf(str.charAt(i)) >= 0) {
        sb.append("\\");
      }
      sb.append(str.charAt(i));
    }
    return sb.toString();
  }

  @Override
  public boolean acceptsEmptyWord() {
    return text.length() == 0;
  }

  @Override
  public String toRegexString(ToStringModifier m) {
    return ToStringModifier.apply(m, this, text);
  }

  @Override
  public int height() {
    return 0;
  }

  @Override
  public String toJava() {
    return "new Text(" + Tools.asJavaString(text) + ")";
  }

  @Override
  public String nodeText() {
    return text;
  }

  @Override
  protected void leaves(List<Expr> l) {
    l.add(this);
  }

  @Override
  public Expr transform(Transform fn, Expr oldParent) {
    return fn.apply(this, new Text(text, false), oldParent);
  }

}