package fr.telecom_paristech.dbweb.regexrepair.matcher.myers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.data.TextSpan;
import fr.telecom_paristech.dbweb.regexrepair.iface.Matching;
import fr.telecom_paristech.dbweb.regexrepair.matcher.Matcher;

/**
 * Implements an approximative regex matching algorithm.
 * See [1] for more details.
 * 
 * Quantifiers are unwrapped (a{3} -&gt; aaa).
 * 
 * [1] E. W. Myers and W. Miller, “Approximate matching of regular expressions,” Bulletin of mathematical biology, vol. 51, no. 1, pp. 5–37, 1989.
 */
public class MyersMatcher implements Matcher {

  /* Constants of the algorithm, see [1] */
  private final float g = 1;

  private final float weight_insert = -1;

  private final float weight_delete = -1;

  private final float weight_subst = Float.NEGATIVE_INFINITY;

  /** Addition to original algorithm */
  private final float weight_match = 2;

  private final float weight_match_special_chars = 2f;

  private static final java.util.regex.Pattern specialChars = java.util.regex.Pattern.compile("\\p{Punct}");

  /** V in topological order */
  private List<State> V;

  /** V without \theta in topological order */
  private List<State> V_tail;

  /** V - {t : \lambda(t) = \epsilon and t != \theta } in topological order */
  private List<State> V_D;

  /** Finate state automaton representation of the regex */
  private Automaton a;

  /** A cell of the dynamic programming table */
  private class Cell {

    /** Weight of the cell */
    float val;

    /** Position of the cell relative to the string */
    int i;

    /** Position of the cell relative to the regex */
    int j;

    /** For a string representation */
    char matrix = ' ';

    /** The cell that lead to the last update on 'val' */
    Cell prev = null;

    /** Reference to the leaf of the regex corresponding to this cell */
    Expr e;

    /** Reference to the character of the string corresponding to this cell */
    TextSpan s;

    /** Whether s and e match */
    boolean matches = false;

    /** What happened to this cell, for debugging */
    String info = " ";

    Cell(float val) {
      this.val = val;
    }

    Cell(float val, int i, int j, char matrix) {
      this(val);
      this.i = i;
      this.j = j;
      this.matrix = matrix;
    }

    /** Add a value to this cell */
    Cell plus(float f) {
      Cell r = new Cell(val + f);
      r.prev = prev;
      return r;
    }

    /** Set this cell to value of other cell */
    void set(Cell other) {
      if (other != null) {
        this.val = other.val;
        if (other != this) {
          this.prev = other;
        }
      }
    }

    /** Copy this cell */
    Cell copy() {
      Cell r = new Cell(val, i, j, matrix);
      r.prev = prev;
      r.info = info;
      return r;
    }

    /** Weight, if we would ignore regex leaf e */
    Cell insert(Expr e) {
      Cell r = this.copy();
      r.info = "ins " + e;
      r.val += weight_insert;
      r.e = e;
      return r;
    }

    /** Weight, if we would ignore string character s */
    Cell delete(TextSpan s) {
      Cell r = this.copy();
      r.info = "del " + s;
      r.val += weight_delete;
      r.s = s;
      return r;
    }

    /** Weight, if we would match or substitute s with e */
    Cell subst(TextSpan s, Expr e) {
      Cell r = this.copy();
      if (e != null && com.google.re2j.Pattern.compile(e.toRegexString()).matches(s.spanString())) {
        if (weight_match_special_chars == weight_match) {
          r.val += weight_match;
        } else {
          r.val += specialChars.matcher(s.spanString()).matches() ? weight_match_special_chars : weight_match;
        }
        r.info = "mch " + s + "->" + e;
        r.matches = true;
      } else {
        r.val += weight_subst;
        r.info = "sub " + s + "->" + e;
      }
      r.s = s;
      r.e = e;
      return r;
    }

    @Override
    public String toString() {
      String prevInfo = "          ";
      if (prev != null) {
        prevInfo = " -> " + prev.i + "," + prev.j + "," + prev.matrix;
      }
      return "" + info.charAt(0) + " " + (val == Float.NEGATIVE_INFINITY ? " -inf" : String.format("%5d", (int) val)) + prevInfo;
    }
  }

  /** Cells at the border */
  Cell negInf = new Cell(Float.NEGATIVE_INFINITY);

  /** Initialize matcher for regex (create automaton) */
  private static MyersMatcher matcher(Expr expr) {
    MyersMatcher result = new MyersMatcher();

    Map<Expr, State> exprToState = new HashMap<>();
    Automaton a = Automaton.walk(expr, exprToState);
    // transform to F_R'
    if (a.states.size() == 0) {
      State n = new State();
      a.states.add(0, n);
      a.start = n;
      a.end = n;
    } else if (!a.start.empty()) {
      State n = new State();
      a.states.add(0, n);
      a.start.prev.add(n);
      a.start.prevDAG.add(n);
      a.start = n;
    }
    result.a = a;
    result.V = a.states;
    result.V_D = new ArrayList<>(a.states);
    result.V_D.removeIf(t -> t.label.isEmpty() && t != a.start);
    result.V_tail = result.V.subList(1, result.V.size());
    int i = 0;
    for (State s : a.states) {
      s.index = i++;
    }

    return result;
  }

  /** Calculate matching from string to regex */
  public Matching match(String str) {
    // declare tables
    int M = str.length();
    Cell[][] C = new Cell[str.length() + 1][a.states.size()];
    Cell[][] D = new Cell[str.length() + 1][a.states.size()];
    Cell[][] I = new Cell[str.length() + 1][a.states.size()];

    // initialize border
    for (int i = 0; i < str.length() + 1; i++) {
      for (int j = 0; j < a.states.size(); j++) {
        C[i][j] = new Cell(Float.NEGATIVE_INFINITY, i, j, 'C');
        D[i][j] = new Cell(Float.NEGATIVE_INFINITY, i, j, 'D');
        I[i][j] = new Cell(Float.NEGATIVE_INFINITY, i, j, 'I');
      }
    }

    // apply algorithm in Figure 7 of [1]
    C[0][a.start.index].set(new Cell(0)); // l.1
    for (State s : V_D) {
      D[0][s.index].set(negInf); // l.3
    }
    for (State s : V_tail) {
      C[0][s.index].set(max(C, 0, filter(s.prevDAG, t -> s.empty()))); // l.5
    }
    I[0][a.start.index].set(negInf); // l.6
    for(State s : V_tail) {
      I[0][s.index].set(max(max(I, 0, s.prevDAG), max(C, 0, filter(s.prev, t -> !s.empty())).plus(-g)).insert(s.expr)); // l.8
    }
    for(State s : V_tail) {
      I[0][s.index].set(max(I[0][s.index], max(I, 0, filter(s.prev, t -> t != a.start)).insert(s.expr))); // l.10
    }
    for (State s : V_tail) {
      C[0][s.index].set(max(C[0][s.index], I[0][s.index])); // l.12
    }
    for (int i=1; i <= M; i++) {
      TextSpan ai = new TextSpan(str, i - 1, i);
      for (State s : V_D) {
        D[i][s.index].set(max(C[i - 1][s.index].plus(-g), D[i - 1][s.index]).delete(ai)); // l.15
      }
      for (State s : V) {
        if (s == a.start || !s.empty()) {
          C[i][s.index].set(max(D[i][s.index], max(C, i - 1, s.prev).subst(ai, s.expr))); // l.18
        } else {
          C[i][s.index].set(max(C, i, s.prevDAG)); // l.20
        }
      }
      I[i][a.start.index].set(negInf); // l. 23
      for (State s : V_tail) {
        I[i][s.index].set(max(max(I, i, s.prevDAG), max(C, i, filter(s.prev, t -> !s.empty())).plus(-g)).insert(s.expr)); // l.25
      }
      for (State s : V_tail) {
        I[i][s.index].set(max(I[i][s.index], max(I, i, filter(s.prev, t -> t != a.start)).insert(s.expr))); // l.27
      }
      for (State s : V_tail) {
        C[i][s.index].set(max(C[i][s.index], I[i][s.index])); // l.29
      }
    }

    // trace way back
    Matching m = new Matching();
    m.matches = new ArrayList<>();
    m.str = str;
    Cell act = C[M][a.end.index];
    for (int i = 0; i < str.length(); i++) {
      m.matches.add(null);
    }
    while (act != null && act.prev != act) {
      if (act.matches) {
        m.matches.set(act.i, act.e);
      }
      act = act.prev;
    }

    return m;
  }

  /** Helper function to filter list of states based on a predicate */
  private List<State> filter(List<State> prevDAG, Function<State, Boolean> cnd) {
    return prevDAG.stream().filter(cnd::apply).collect(Collectors.toList());
  }

  /** Find cell with the highest value, representing the result of the algorithm */
  private Cell max(Cell[][] matrix, int row, List<State> states) {
    float m = negInf.val;
    Cell max = negInf;
    for (State t : states) {
      Cell act = matrix[row][t.index];
      if (m < act.val) {
        max = act;
        m = act.val;
      }
    }
    return max;
  }

  /** Cell with higher value */
  private Cell max(Cell a, Cell b) {
    if (a == null) {
      return b;
    }
    if (b == null) {
      return a;
    }
    float m = Math.max(a.val, b.val);
    return m == a.val ? a : b;
  }

  @Override
  public Matching match(Expr e, String s) {
    return matcher(e).match(s);
  }

  @Override
  public String info() {
    return "myers";
  }

  public static void main(String[] args) {
    MyersMatcher m;
    m = matcher(RegexParser.parse("\\d{2}/\\d{2}/\\d{4}"));
    System.out.println(m.match("April 20, 2001"));
  }
}
