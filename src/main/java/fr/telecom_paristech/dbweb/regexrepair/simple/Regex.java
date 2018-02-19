package fr.telecom_paristech.dbweb.regexrepair.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a regular expression.
 * 
 * @author Fabian M. Suchanek
 *
 */
public class Regex {

  /**
   * Type of nodes in a regex
   */
  public enum NodeType {
    CONCATENATION, DISJUNCTION, KLEENESTAR, CHARACTERRANGE, CHARACTER, EMPTY
  }

  /** Type of this node*/
  public NodeType type = NodeType.EMPTY;

  /** Points to parent node */
  public Regex parent;

  /** Index of this child in the parent */
  public int childNumber;

  /** Children (or null for leaf nodes) */
  public List<Regex> children;

  /** Value of this node (for Character nodes etc.) */
  public String value;

  /** j-index in the Meyer Matrix. Used exclusively there.*/
  public int jIndex;

  /** Returns the set of predecessors */
  public Set<Regex> previous() {
    try {
      HashSet<Regex> result = new HashSet<>();
      parent.previousOfChild(childNumber, result);
      result.removeIf(r -> r == null || (r.children != null && r.children.size() > 0));
      return (result);
    } catch (Exception | StackOverflowError e) {
      Regex root = this;
      System.err.println("some problem in regex: previous() called on ");
      while (true) {
        System.err.println(root.toPrettyString() + " childNumber " + root.childNumber);
        if (root.parent == null || root.parent == root) {
          break;
        }
        root = root.parent;
      }
      e.printStackTrace();
    }
    return null;
  }

  /** Returns the set of predecessors of the ith child */
  protected void previousOfChild(int i, Set<Regex> result) {
    if (value != null) {
      return;
    }
    Regex c = children.get(i);
    if (result.contains(c)) {
      return;
    }
    if (c.type == NodeType.KLEENESTAR) {
      result.add(c);
    }
    switch (type) {
      case CONCATENATION:
        if (i > 0) {
          children.get(i - 1).exitLeafNodes(result);
        } else if (parent != null) {
          parent.previousOfChild(childNumber, result);
        }
        break;
      case DISJUNCTION:
        parent.previousOfChild(childNumber, result);
        break;
      case KLEENESTAR:
        parent.previousOfChild(childNumber, result);
        children.get(0).exitLeafNodes(result);
        break;
      case CHARACTER:
      case CHARACTERRANGE:
        throw new RuntimeException("Cannot determine predecessor of i-th child for a character node " + this);
      case EMPTY:
        parent.previousOfChild(childNumber, result);
        break;
    }
    ;
  }

  /** All nodes contained in this regex (including this node) */
  protected Set<Regex> allNodes() {
    Set<Regex> result = new HashSet<>();
    allNodes(result);
    return result;
  }

  /** All nodes contained in this regex (including this node) */
  protected void allNodes(Set<Regex> result) {
    if (result.contains(this)) {
      return;
    }
    result.add(this);
    if (children != null) {
      for (Regex child : children) {
        child.allNodes(result);
      }
    }
  }

  /** Returns the final leaf nodes of this node */
  protected void exitLeafNodes(Set<Regex> result) {
    switch (type) {
      case CONCATENATION:
        children.get(children.size() - 1).exitLeafNodes(result);
        return;
      case DISJUNCTION:
        for (Regex child : children) {
          child.exitLeafNodes(result);
        }
        return;
      case KLEENESTAR:
        children.get(0).exitLeafNodes(result);
        parent.previousOfChild(childNumber, result);
        return;
      case CHARACTER:
      case CHARACTERRANGE:
        result.add(this);
        return;
      case EMPTY:
        if (value != null) {
          return;
        }
        value = "No recursion";
        result.addAll(parent.previous());
        value = null;
        return;
    }
  }

  /** Constructs a regex of a certain type*/
  public Regex(NodeType c) {
    type = c;
  }

  /** Constructs a regex of an empty concatenation */
  public Regex() {
    type = NodeType.EMPTY;
  }

  /** Copies a regex */
  public Regex copy() {
    return copy(null);
  }

  /** Copies a regex with provenance */
  public Regex copy(Map<Regex, Regex> newToOld) {
    Regex result = new Regex();
    result.adopt(this, newToOld);
    if (newToOld != null) {
      newToOld.put(result, this);
    }
    return (result);
  }

  /** Copies a regex to this */
  public void adopt(Regex regex, Map<Regex, Regex> newToOld) {
    this.type = regex.type;
    this.value = regex.value;
    if (regex.children != null) {
      this.children = new ArrayList<>();
      for (Regex child : regex.children) {
        this.children.add(child.copy(newToOld));
      }
    }
    this.setParentPointers();
  }

  /** Constructs a concatenation */
  public Regex(List<Regex> children) {
    type = NodeType.CONCATENATION;
    this.children = children;
    setParentPointers();
  }

  /** Constructs a regex */
  public Regex(NodeType value, Regex... children) {
    this.type = value;
    this.children = new ArrayList<>(Arrays.asList(children));
    setParentPointers();
  }

  /** Adjusts parent pointers */
  protected void simplify() {
    if (type == NodeType.CONCATENATION && children.isEmpty()) {
      type = NodeType.EMPTY;
      value = null;
      children = null;
    }
    if ((type == NodeType.CONCATENATION || type == NodeType.DISJUNCTION) && children.size() == 1) {
      value = children.get(0).value;
      type = children.get(0).type;
      children = children.get(0).children;
    }
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        Regex child = children.get(i);
        if (type == NodeType.DISJUNCTION && child.type == NodeType.DISJUNCTION
            || type == NodeType.CONCATENATION && child.type == NodeType.CONCATENATION) {
          children.remove(i);
          children.addAll(i, child.children);
          i--;
          continue;
        }
        child.childNumber = i;
        child.parent = this;
        child.simplify();
      }
    }
  }

  /** Adjusts parent pointers */
  protected void setParentPointers() {
    if (type == NodeType.CONCATENATION && children.isEmpty()) {
      type = NodeType.EMPTY;
      value = null;
      children = null;
    }
    if (children != null) {
      for (int i = 0; i < children.size(); i++) {
        Regex child = children.get(i);
        child.childNumber = i;
        child.parent = this;
        child.setParentPointers();
      }
    }
  }

  /** Builds a regex from a string */
  public Regex(String s) {
    this(s, new int[1]);
    simplify();
  }

  /** Pattern for {., .} ranges */
  protected static final Pattern iterationPattern = Pattern.compile("\\{\\s*(\\d+)\\s*(,\\s*(\\d*)\\s*)?\\}");

  /** Builds a regex from a string */
  protected Regex(String s, int[] i) {
    type = NodeType.CONCATENATION;
    children = new ArrayList<>();
    while (i[0] < s.length()) {
      switch (s.charAt(i[0])) {
        case '{':
          Matcher m = iterationPattern.matcher(s);
          m.region(i[0], s.length());
          if (!m.find()) {
            throw new RuntimeException("Misformed iteration {.,.}: " + s.substring(i[0]));
          }
          int start = Integer.parseInt(m.group(1));
          int end = m.group(2) == null || m.group(3).isEmpty() ? start : Integer.parseInt(m.group(3));
          Regex child = children.get(children.size() - 1);
          children.remove(children.size() - 1);
          for (int k = 0; k < start; k++) {
            children.add(child.copy());
          }
          Regex optionalChild = new Regex(NodeType.DISJUNCTION, child, new Regex());
          for (int k = start; k < end; k++) {
            children.add(optionalChild.copy());
          }
          if (m.group(2) != null && m.group(3).isEmpty()) {
            children.add(new Regex(NodeType.KLEENESTAR, child.copy()));
          }
          i[0] = m.end();
          break;
        case '?':
          Regex newDisjunction = new Regex(NodeType.DISJUNCTION, new ArrayList<>(Arrays.asList(children.get(children.size() - 1), new Regex())));
          children.set(children.size() - 1, newDisjunction);
          i[0]++;
          break;
        case '+':
          Regex lastChild = children.get(children.size() - 1);
          children.add(new Regex(NodeType.KLEENESTAR, lastChild.copy()));
          i[0]++;
          break;
        case '(':
          i[0]++;
          children.add(new Regex(s, i));
          continue;
        case ')':
          i[0]++;
          return;
        case '|':
          i[0]++;
          children = new ArrayList<>(Arrays.asList(this.copy(), new Regex(s, i)));
          type = NodeType.DISJUNCTION;
          i[0]--; // Let them parse the closing ')'
          continue;
        case '*':
          i[0]++;
          lastChild = children.get(children.size() - 1);
          children.set(children.size() - 1, new Regex(NodeType.KLEENESTAR, lastChild));
          continue;
        case '[':
          Regex r = new Regex();
          r.type = NodeType.CHARACTERRANGE;
          int endPos = s.indexOf(']', i[0]) + 1;
          r.value = s.substring(i[0], endPos);
          i[0] = endPos;
          children.add(r);
          continue;
        case '.':
          r = new Regex();
          r.type = NodeType.CHARACTERRANGE;
          r.value = ".";
          children.add(r);
          i[0]++;
          continue;
        case '\\':
          i[0]++;
          if ("dwsDWS".indexOf(s.charAt(i[0])) != -1) {
            r = new Regex();
            r.type = NodeType.CHARACTERRANGE;
            r.value = "\\" + s.charAt(i[0]);
            children.add(r);
            i[0]++;
            continue;
          }
          // Fall through
        default:
          children.add(new Regex(s.charAt(i[0])));
          i[0]++;
      }
    }
    return;
  }

  public static Regex quoted(String s) {
    switch (s.length()) {
      case 0:
        return (new Regex());
      case 1:
        return (new Regex(s.charAt(0)));
      default:
        Regex result = new Regex();
        result.children = new ArrayList<>(s.length());
        result.type = NodeType.CONCATENATION;
        for (int i = 0; i < s.length(); i++) {
          result.children.add(new Regex(s.charAt(i)));
        }
        result.setParentPointers();
        return (result);
    }
  }

  public Regex(char c) {
    this.type = NodeType.CHARACTER;
    this.value = "" + c;
  }

  public Regex(NodeType type, List<Regex> children) {
    this.type = type;
    this.children = children;
    setParentPointers();
  }

  /** Creates a string of the regex */
  protected void toString(StringBuilder b) {
    switch (type) {
      case KLEENESTAR:
        b.append("(");
        b.append(children.get(0));
        b.append(")*");
        break;
      case CONCATENATION:
        b.append("(");
        for (Regex child : children) {
          b.append(child);
        }
        b.append(")");
        break;
      case DISJUNCTION:
        b.append("(");
        for (Regex child : children) {
          child.toString(b);
          b.append('|');
        }
        b.setLength(b.length() - 1);
        b.append(")");
        break;
      case CHARACTER:
        char c = value.charAt(0);
        if ("()*+?\\{}[]|.".indexOf(c) != -1) {
          b.append("\\");
        }
        b.append(c);
        break;
      case CHARACTERRANGE:
        b.append(value);
        break;
      default:
        break;
    }
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    toString(b);
    return (b.toString());
  }

  /**
   * Makes the children between i and j (excl.) optional. Returns the disjunction that was added
   */
  public Regex makeChildrenOptionalAndAdd(int i, int j, Regex add) {
    Regex disjunction;
    switch (type) {
      case DISJUNCTION:
        return (children.get(j - 1).makeChildrenOptionalAndAdd(0, 0, add));
      case EMPTY:
      case CHARACTER:
      case CHARACTERRANGE:
        Regex concat;
        Regex realParent = parent;
        int realChildNumber = childNumber;
        if (i == 0 && j == 0) {
          concat = new Regex(NodeType.CONCATENATION, disjunction = new Regex(NodeType.DISJUNCTION, new Regex(), add), this);
        } else if (i == 0 && j == 1) {
          concat = disjunction = new Regex(NodeType.DISJUNCTION, add, this);
        } else {
          concat = new Regex(NodeType.CONCATENATION, this, disjunction = new Regex(NodeType.DISJUNCTION, new Regex(), add));
        }
        realParent.children.set(realChildNumber, concat);
        realParent.setParentPointers();
        return (disjunction);
      case CONCATENATION:
        if (j == Integer.MAX_VALUE) {
          j = children.size();
        }
        if (i > j) {
          j = i;
        }
        if (i == j && add.type == NodeType.EMPTY) {
          setParentPointers();
          return (null);
        }
        Regex newConjunction = new Regex(NodeType.CONCATENATION, new ArrayList<>(children.subList(i, j)));
        disjunction = new Regex(NodeType.DISJUNCTION, newConjunction, add);
        if (i == j) {
          children.add(i, disjunction);
        } else {
          j--;
          for (; j > i; j--) {
            children.remove(j);
          }
          children.set(i, disjunction);
        }
        setParentPointers();
        return (disjunction);
      case KLEENESTAR:
        return (children.get(0).makeChildrenOptionalAndAdd(0, 0, add));
    }
    return null;
  }

  /** Returns a list of leaf nodes */
  public List<Regex> leafNodes() {
    List<Regex> result = new ArrayList<>();
    leafNodes(result);
    return (result);
  }

  /** Returns a list of leaf nodes */
  protected void leafNodes(List<Regex> result) {
    if (children == null) {
      if (this.type != NodeType.EMPTY) {
        result.add(this);
      }
    } else {
      for (Regex child : children) {
        child.leafNodes(result);
      }
    }
  }

  /** TRUE if this regex matches this char */
  public boolean matchesChar(char c) {
    switch (type) {
      case CHARACTER:
        return (value.charAt(0) == c);
      case CHARACTERRANGE:
        // Exclude the special marker of Repair.java
        if (c == 0x27E6 || c == 0x27E7) {
          return (false);
        }
        return ((c + "").matches(value));
      default:
        return (false);
    }
  }

  /** Returns a pretty string*/
  public String toPrettyString() {
    return (new CountedRegex(this, null).toString());
  }

  /** Returns a generalized string*/
  public String toGeneralizedString() {
    CountedRegex cr = new CountedRegex(this, null);
    cr.generalize();
    return (cr.toString());
  }

  /** Returns a string in which the counters are generalized*/
  public String toCounterGeneralizedString() {
    CountedRegex cr = new CountedRegex(this, null);
    cr.generalizeCounters();
    return (cr.toString());
  }

  /** Returns the child number below the given parent (or -1)*/
  public int childNumberBelow(Regex parent) {
    if (this.parent == parent) {
      return (childNumber);
    }
    if (this.parent == null) {
      return (-1);
    }
    return (this.parent.childNumberBelow(parent));
  }

  /** Test method */
  public static void main(String[] args) {
    String regex = "(a?)*";
    String word = "aba";
    Repair.add(word, regex);
    new Regex("$(a*)*$").children.get(1).children.get(0).previous();
  }

  /** TRUE if this regex is a special character*/
  public boolean isSpecialChar() {
    switch (type) {
      case CHARACTER:
        return (value.matches("\\p{Punct}"));
      default:
        return false;
    }

  }
}