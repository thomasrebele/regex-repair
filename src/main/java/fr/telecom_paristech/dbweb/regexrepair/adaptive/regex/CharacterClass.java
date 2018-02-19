package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;

public class CharacterClass extends Expr {

  public String content;

  public CharacterClass() {

  }

  public CharacterClass(String content) {
    this.content = content;
  }

  @Override
  public boolean acceptsEmptyWord() {
    return false;
  }

  // functions related to string representations

  @Override
  public String toRegexString(ToStringModifier m) {
    return ToStringModifier.apply(m, this, content);
  }

  @Override
  public int height() {
    return 0;
  }

  @Override
  public String toJava() {
    return "new CharacterClass(" + Tools.asJavaString(content) + ")";
  }

  @Override
  public String nodeText() {
    return content;
  }

  @Override
  protected void leaves(List<Expr> l) {
    l.add(this);
  }

  @Override
  public Expr transform(Transform fn, Expr oldParent) {
    return fn.apply(this, new CharacterClass(content), oldParent);
  }

  public static String from(String chars) {
    if (chars.length() == 1) {
      return Text.escape(chars);
    }
    return "[" + chars.replace("^", "\\^").replace("\\", "\\\\").replace("&", "\\&").replace("-", "\\-") + "]";
  }
}
