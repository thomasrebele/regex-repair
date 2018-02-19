package fr.telecom_paristech.dbweb.regexrepair.adaptive.regex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.parser.RegexGrammarLexer;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.parser.RegexGrammarParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.parser.RegexGrammarParser.Shared_atomContext;
import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;

/**
 * Parse a regular expression to an abstract syntax tree.
 * It uses the grammar from https://github.com/bkiers/pcre-parser
 */
public class RegexParser {

  private final static Logger log = LoggerFactory.getLogger(RegexParser.class);

  private static ParseTree parseToTree(String s) {
    // adding (regex) fixes parsing of a|b
    CharStream input = new ANTLRInputStream("(" + s + ")");
    RegexGrammarLexer lexer = new RegexGrammarLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    RegexGrammarParser parser = new RegexGrammarParser(tokens);
    RegexGrammarParser.ExprContext tree = null;
    try {
      tree = parser.expr();
    } catch (Exception e) {
      log.error("Cannot parse regex " + Tools.asJavaString(s));
      e.printStackTrace();
      System.exit(0);
    }
    return tree;
  }

  /** Parse a String to a Regex.Expr tree */
  public static Expr parse(String s) {

    Expr expr = null;
    try {
      expr = RegexParser.transform(parseToTree(s));
    } catch (Exception e) {
      log.error("Cannot parse regex " + Tools.asJavaString(s));
      e.printStackTrace();
      System.exit(0);
    }

    return expr;
  }

  /**
   * Print an ANTLR concrete syntax tree.
   * Visualizes parse tree of ANTLR.
   *
   * @param ctx
   * @param indentation
   */
  private static void explore(RuleContext ctx, int indentation) {
    String ruleName = RegexGrammarParser.ruleNames[ctx.getRuleIndex()];
    for (int i = 0; i < indentation; i++) {
      System.out.print("  ");
    }
    System.out.println(ruleName);
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree element = ctx.getChild(i);
      if (element instanceof RuleContext) {
        explore((RuleContext) element, indentation + 1);
      }
      else {
        for (int j = 0; j < indentation + 1; j++) {
          System.out.print("  ");
        }
        System.out.println(element.getClass().getSimpleName() + ": " + element.getText());
      }
    }
  }

  /**
   * A map from variable names of right hand side of rule to its parse trees
   * @param ctx
   * @return
   */
  private static HashMap<String, List<RuleContext>> childMap(ParseTree ctx) {
    HashMap<String, List<RuleContext>> result = new HashMap<>();
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree element = ctx.getChild(i);
      if (element instanceof RuleContext) {
        String ruleName = RegexGrammarParser.ruleNames[((RuleContext) element).getRuleIndex()];
        result.computeIfAbsent(ruleName, k -> new ArrayList<>()).add((RuleContext) element);
      }
    }
    return result;
  }

  /**
   * Transform first variable of right hand of rule to an expression.
   * If not possible, return whole tree as an expression.
   * @param ctx
   * @return
   */
  private static Expr transformFirst(ParseTree ctx) {
    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree child = ctx.getChild(i);
      if (child instanceof RuleContext) {
        Expr result = transform(child);
        if (result != null) {
          return result;
        }
      }
    }
    CharacterClass cc = new CharacterClass();
    cc.content = ctx.getText();
    return cc;
  }

  /**
   * Take a ANTLR parse tree and transform it to a regular expression
   * @param ctx
   * @return
   */
  private static Expr transform(ParseTree ctx) {
    AggExpr e = null;
    Expr result = null;

    log.trace("transforming {} '{}'", ctx.getClass(), ctx.getText());

    if (ctx instanceof RegexGrammarParser.LetterContext || ctx instanceof RegexGrammarParser.DigitContext || ctx instanceof Shared_atomContext
        || ctx instanceof RegexGrammarParser.Shared_literalContext) {
      return new Text(ctx.getText(), false);
    } else if (ctx instanceof RegexGrammarParser.AlternationContext) {
      e = new Alt();
    } else if (ctx instanceof RegexGrammarParser.ExprContext) {
      e = new Conc();
    } else if (ctx instanceof RegexGrammarParser.Character_classContext) {
      CharacterClass cc = new CharacterClass();
      cc.content = ctx.getText();
      result = cc;
    } else if (ctx instanceof RegexGrammarParser.ElementContext) {
      // repeat
      result = transform(ctx.getChild(0));
      if (ctx.getChildCount() == 2) {
        ParseTree quant = ctx.getChild(1);
        if (quant.getText().startsWith("*")) {
          e = new Repeat(0, Repeat.STAR);
        }
        else if (quant.getText().startsWith("+")) {
          e = new Repeat(1, Repeat.STAR);
        }
        else if (quant.getText().startsWith("?")) {
          e = new Repeat(0, 1);
        }
        else if (quant.getText().startsWith("{")) {
          Repeat r = new Repeat();
          e = r;
          r.setMin(Integer.parseInt(quant.getChild(1).getText()));
          if (quant.getChildCount() == 4) {
            r.setMax(r.getMin());
          } else if (quant.getChildCount() == 5) {
            r.setMax(Repeat.STAR);
          }
          else if (quant.getChildCount() == 6) {
            r.setMax(Integer.parseInt(quant.getChild(3).getText()));
          }
        } else {
          log.error("RegexParser doesn't support quantifier " + quant.getText());
        }
        e.addChild(result);
        result = e;
        e = null;
      }
    } else if (ctx instanceof RegexGrammarParser.AtomContext || ctx instanceof RegexGrammarParser.LiteralContext
        || ctx instanceof RegexGrammarParser.CaptureContext
        || ctx instanceof RegexGrammarParser.Non_captureContext) {
      result = transformFirst(ctx);
    } 
    else if (ctx instanceof TerminalNodeImpl) {
      // ignore
    }
    else {
      log.error("RegexParser doesn't support " + ctx.getClass());
    }

    if (e != null) {
      result = transformChildren(e, ctx, false);
    } else {
    }
    String r = result == null ? "null" : result.toRegexString();
    log.trace("transformed {} '{}', result " + r, ctx.getClass(), ctx.getText());
    return result;
  }

  /**
   * Add children of parse tree to an aggregated expression.
   * Avoid unnecessarily nested expressions
   * @param e
   * @param ctx
   * @param addText
   * @return
   */
  private static Expr transformChildren(AggExpr e, ParseTree ctx, boolean addText) {
    boolean collapseSingleChild = true && (e == null || e.getCollapseSingleChild());

    for (int i = 0; i < ctx.getChildCount(); i++) {
      ParseTree childTree = ctx.getChild(i);
      Expr child = transform(childTree);
      if (collapseSingleChild && ctx.getChildCount() == 1) {
        return child;
      }

      if (child != null) {
        e.addChild(child);
      }
    }

    return e;
  }

  public static void main(String[] args) {
    String r = "(ab|cd){0,3}";
    //ParseTree tree = parseToTree(r);
    //explore((RuleContext) tree, 0);
    Expr e = RegexParser.parse(r);
    System.out.println(e.print());
    System.out.println(e.toRegexString());
  }

}
