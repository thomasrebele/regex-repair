package fr.telecom_paristech.dbweb.regexrepair.matcher.myers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;

/**
 * Represents a finite state automaton (ε-NFA).
 * It is based on [1]. Characters are mapped to states, not transitions.
 * [1] E. W. Myers and W. Miller, “Approximate matching of regular expressions,” Bulletin of mathematical biology, vol. 51, no. 1, pp. 5–37, 1989.
 */
class Automaton {

  /** States in DAG topological order */
  List<State> states = new ArrayList<>();

  /** The start (or first) state of the automaton */
  State start;

  /** The end (or final) state of the automaton */
  State end;

  /**
   * Walk over regex to generate automaton
   * @param expr
   * @param leafToState accumulator, mapping from leaf nodes of the regular expression to their state
   */
  static Automaton walk(Expr expr, Map<Expr, State> leafToState) {
    // th (theta): start of sub-automaton
    // phi: end of sub-automaton

    Automaton a = new Automaton();
    if (expr instanceof Conc) {
      // th=th1--(automata1)-->phi1 --> th2--(automata2)-->phi2=phi
      for (Expr child : ((AggExpr) expr).getChildren()) {
        Automaton c = walk(child, leafToState);
        a.states.addAll(c.states);
        if (a.start == null) {
          a.start = c.start;
        }
        else {
          a.end.transition(c.start, true);
        }
        a.end = c.end;
      }

    } else if (expr instanceof Alt) {
      //      /-->th1---(automata1)--->phi1---\
      //  th--                                 &-->phi
      //      \-->th2---(automata2)--->phi2---/
      a.start = new State();
      a.end = new State();
      a.states.add(a.start);
      for (Expr child : ((AggExpr) expr).getChildren()) {
        Automaton c = walk(child, leafToState);
        a.start.transition(c.start, true);
        c.end.transition(a.end, true);
        a.states.addAll(c.states);
      }
      a.states.add(a.end);

    } else if (expr instanceof Repeat) {
      Repeat r = (Repeat) expr;
      // kleene star
      if (r.getMin() == 0 && r.getMax() == Repeat.STAR) {
        //   /--------------------------------\
        // th---> th1---(automata)---->phi1----&->psi
        //          ^------------------/
        a.start = new State();
        a.end = new State();
        a.states.add(a.start);
        a.start.transition(a.end, true);
        Automaton c = walk(((Repeat) expr).getChild(), leafToState);
        c.end.transition(c.start, false);
        a.start.transition(c.start, true);
        c.end.transition(a.end, true);
        a.states.addAll(c.states);
        a.states.add(a.end);

      } else {
        // unfold min times
        // (example: unfold 2x)
        // th=th1---(automata)--->phi1--->th2---(automata)--->psi2=psi
        for (int i = 0; i < r.getMin(); i++) {
          Automaton c = walk(r.getChild(), leafToState);
          a.states.addAll(c.states);
          if (a.start == null) {
            a.start = c.start;
          } else {
            a.end.transition(c.start, true);
          }
          a.end = c.end;
        }

        // treat max
        if (r.getMax() == Repeat.STAR) {
          if (a.start == null) {
            a.start = new State();
            a.end = a.start;
            a.states.add(a.start);
          }

          //        /--------------------------------\
          // last_psi---> th1---(automata)---->phi1------>psi
          //                ^------------------/
          Automaton c = walk(r.getChild(), leafToState);
          a.states.addAll(c.states);
          a.end.transition(c.start, true);
          c.end.transition(c.start, false);
          State n = new State();
          a.states.add(n);
          a.end.transition(n, true);
          c.end.transition(n, true);
          a.end = n;

        } else if (r.getMax() > r.getMin()) {
          // unfold max times
          // example: unfold 2x
          //        /----------------------------------&--------------------------------\
          // last_psi---> th1---(automata)---->phi1---/---> th2---(automata)---->phi2----&->psi
          //                
          State n = new State(); // new final state
          if (a.start == null) { // create start state if not existing
            a.start = new State();
            a.end = a.start;
            a.states.add(a.start);
          }
          for (int i = r.getMin(); i < r.getMax(); i++) {
            Automaton c = walk(r.getChild(), leafToState);
            a.states.addAll(c.states);
            a.end.transition(n, true);
            a.end.transition(c.start, true);
            a.end = c.end;
          }
          a.end.transition(n, true);
          a.end = n;
          a.states.add(n);
        }
      }

    } else {
      // th=phi (label: character (class) of regex)
      State s = new State();
      s.label = expr.toRegexString();
      a.start = s;
      a.end = s;
      a.states.add(s);
      s.expr = expr;
      leafToState.put(expr, s);
    }
    if (a.start == null) {
      a.start = new State();
      a.end = a.start;
      a.states.add(a.start);
    }
    return a;
  }
}