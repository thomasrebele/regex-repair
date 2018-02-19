package fr.telecom_paristech.dbweb.regexrepair.matcher.myers;

import java.util.ArrayList;
import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;

/**
 * Represents a state of a finite state automaton
 */
class State {

  /** Character (or character class) of the state */
  String label = "";

  /** Reference to the regex leaf that corresponds to this automaton state */
  Expr expr;

  /** Unique identifier for the state (relative to the automaton) */
  int index = 0;

  /** Edges to previous states */
  List<State> prev = new ArrayList<>();

  /** Subset of edges to previous states. All edges of those kind form a directed acyclic graph on the automaton */
  List<State> prevDAG = new ArrayList<>();

  /** Add transition to this state */
  public void transition(State to, boolean isDag) {
    to.prev.add(this);
    if (isDag) {
      to.prevDAG.add(this);
    }
  }

  /** Check whether this is an ε-state */
  public boolean empty() {
    return label.isEmpty();
  }

  @Override
  public String toString() {
    return index + " " + label;
  }
}