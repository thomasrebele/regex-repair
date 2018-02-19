package fr.telecom_paristech.dbweb.regexrepair.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runs Meyer's algorithm */
public class MyerMatrix {

  /** Regex on which we work */
  public Regex regex;

  /** String on which we work */
  public String string;

  /** Enumeration of all leaf nodes of the regex*/
  public List<Regex> leafNodes;

  /** The matrix */
  public List<Map<Regex, Cell>> matrix = new ArrayList<>();

  /**
   * Runs Meyer's algorithm, Works only if the word and the regex start and
   * end with the same character (e.g., a dummy $)
   */
  public MyerMatrix(String s, Regex r, UpdateFunction f) {
    regex = r;
    string = s;
    leafNodes = r.leafNodes();
    // Set the j index
    for (int j = 0; j < leafNodes.size(); j++) {
      leafNodes.get(j).jIndex = j;
    }
    // Run through all lines (= characters of the string)
    for (int i = 0; i < string.length(); i++) {
      Map<Regex, Cell> previousLine = matrix.isEmpty() ? null : matrix.get(matrix.size() - 1);
      Map<Regex, Cell> currentLine = new HashMap<>();
      matrix.add(currentLine);

      // Run through the regex
      for (int j = 0; j < leafNodes.size(); j++) {
        Regex leafNode = leafNodes.get(j);
        Cell newCell;

        // If s_i \in L(r_j)...
        if (leafNode.matchesChar(string.charAt(i))) {
          if (leafNode.previous().isEmpty()) {
            newCell = new Cell();
            newCell.score = 1;
            newCell.numMatchedCharacters = 1;
            newCell.numMatchedSpecialCharacters = leafNode.isSpecialChar() ? 1 : 0;
            newCell.previousI = -1;
            newCell.previousJ = -1;
          } else {
            Set<Cell> entourage = new HashSet<>();
            for (Regex prev : leafNode.previous()) {
              Cell predecessor = previousLine == null ? new Cell() : previousLine.get(prev);
              // Walk to the next matching predecessor
              Cell walkingPredecessor = predecessor;
              while (!walkingPredecessor.matches()) {
                walkingPredecessor = walkingPredecessor.predecessor();
              }
              newCell = new Cell();
              newCell.numMatchedCharacters = predecessor.numMatchedCharacters + 1;
              newCell.numMatchedSpecialCharacters = predecessor.numMatchedSpecialCharacters + (leafNode.isSpecialChar() ? 1 : 0);
              newCell.previousI = i - 1;
              newCell.previousJ = prev.jIndex;
              newCell.skips = predecessor.skips;
              // If the next matching predecessor is not a previous node, we have skips
              if (!leafNode.previous().contains(walkingPredecessor.leafNode())) {
                newCell.skips++;
              }
              newCell.score = f.score(newCell, predecessor, leafNode);
              entourage.add(newCell);
            }
            newCell = bestOf(entourage);
          }
        } else { // If s_i \not\in L(r_j)...
          Set<Cell> entourage = new HashSet<>();
          if (previousLine != null) {
            entourage.add(previousLine.get(leafNodes.get(j)));
          }
          for (Regex prev : leafNode.previous()) {
            if (previousLine != null) {
              entourage.add(previousLine.get(prev));
            }
            if (prev.jIndex < j) {
              entourage.add(currentLine.get(prev));
            }
          }
          newCell = bestOf(entourage).copy();
        }
        newCell.i = i;
        newCell.j = j;
        currentLine.put(leafNode, newCell);
      }
    }
  }

  /** Reads the matching out from the result */
  public List<Regex> matching() {
    List<Regex> result = new ArrayList<>();
    int i = string.length() - 1;
    int j = leafNodes.size() - 1;
    // Follow the backpointers
    while (j >= 0 && i >= 0) {
      Cell c = cell(i, j);
      if (c.previousI == -1) {
        if (c.score > 0) {
          result.add(leafNodes.get(j));
        } else {
          result.add(null);
        }
      } else if (c.previousI < i) {
        if (c.betterThan(matrix.get(c.previousI).get(leafNodes.get(c.previousJ)))) {
          result.add(leafNodes.get(j));
        } else {
          result.add(null);
        }
      }
      j = c.previousJ;
      i = c.previousI;
    }
    Collections.reverse(result);
    return (result);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder("Myer's matrix for regex " + regex + "\n\n");
    for (int j = 0; j < leafNodes.size(); j++) {
      b.append("\t\t").append(leafNodes.get(j));
    }
    b.append('\n');
    for (int i = 0; i < matrix.size(); i++) {
      b.append(string.charAt(i));
      for (int j = 0; j < matrix.get(i).size(); j++) {
        b.append('\t').append(matrix.get(i).get(leafNodes.get(j)));
      }
      b.append('\n');
    }
    b.append("\t// <score,numMatched,back-i,back-j>\n");
    return (b.toString());
  }

  /** Finds the best cell in a set (or creates a new empty cell) */
  public Cell bestOf(Set<Cell> set) {
    Cell currentBest = null;
    for (Cell c : set) {
      if (currentBest == null || c.betterThan(currentBest)) {
        currentBest = c;
      }
    }
    if (currentBest == null) {
      return (new Cell());
    }
    return (currentBest);
  }

  /** returns the cell at a position*/
  public Cell cell(int i, int j) {
    return (matrix.get(i).get(leafNodes.get(j)));
  }

  /** Represents a cell in the Matrix */
  public class Cell {

    /** My row */
    public int i;

    /** My column */
    public int j;

    /** Backpointer i */
    public int previousI;

    /** Backpointer j */
    public int previousJ;

    /** Score, computed by the update function */
    public int score;

    /** Number of skipped leaf nodes in the regex */
    public int skips;

    /** Number of character matched so far*/
    public int numMatchedCharacters;

    /** Number of special characters matched*/
    public int numMatchedSpecialCharacters;

    /** TRUE if I am better than c */
    public boolean betterThan(Cell c) {
      return (this.score > c.score);
    }

    /** Copies this cell, sets the backpointers to it */
    public Cell copy() {
      Cell copy = new Cell();
      copy.score = this.score;
      copy.previousI = this.i;
      copy.previousJ = this.j;
      copy.numMatchedCharacters = this.numMatchedCharacters;
      copy.numMatchedSpecialCharacters = this.numMatchedSpecialCharacters;
      return (copy);
    }

    /** TRUE if this character matches this leaf node*/
    public boolean matches() {
      return (leafNodes.get(j).matchesChar(string.charAt(i)));
    }

    /** Returns the predecessor*/
    public Cell predecessor() {
      return (cell(previousI, previousJ));
    }

    /** Returns the leaf node*/
    public Regex leafNode() {
      return (leafNodes.get(j));
    }

    @Override
    public String toString() {
      return "<" + score + "," + numMatchedCharacters + "," + previousI + "," + previousJ + ">";
    }
  }

  public static void main(String[] args) {
    System.out.println("Works only if word and regex start and end with the same character");
    // MyerMatrix m = new MyerMatrix("$(123) 456-789$", new
    // Regex("$([0-9][0-9]*-)*$"), new UpdateFunction());
    MyerMatrix m = new MyerMatrix("$aa $", new Regex("$(a[a-z]* )*$"), new UpdateFunction());
    System.out.println(m);
    System.out.println(m.matching());
  }
}
