package fr.telecom_paristech.dbweb.regexrepair.simple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A regex that has a counted range associated to each node */
public class CountedRegex {

  /**
   * Type of nodes in a counted regex
   */
  public enum NodeType {
    CONCATENATION, DISJUNCTION, CHARACTERRANGE, CHARACTER
  }

  /** Type of this node*/
  public NodeType type = NodeType.CONCATENATION;

  /** Children (or null for leaf nodes) */
  public List<CountedRegex> children;

  /** Value of this node (for Character nodes etc.) */
  public String value;

  /** Minimal number of occurrences */
  public int min = 1;

  /** Maximal number of occurrences */
  public int max = 1;

  /** Constant for Kleene star */
  public static final int KLEENE = 1000;

  /** Constructs a counted regex based on a regex */
  public CountedRegex(String r) {
    this("(" + r + ")", new int[1]);
    optimize();
  }

  /** Pattern for {., .} ranges */
  protected static final Pattern iterationPattern = Pattern.compile("\\{\\s*(\\d+)\\s*(,\\s*(\\d*)\\s*)?\\}");

  /** Builds a counted regex from a string */
  protected CountedRegex(String s, int[] i) {
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
          int end = m.group(2) == null ? start : m.group(3).isEmpty() ? KLEENE : Integer.parseInt(m.group(3));
          CountedRegex newChild = new CountedRegex(NodeType.CONCATENATION, children.get(children.size() - 1));
          newChild.min = start;
          newChild.max = end;
          children.set(children.size() - 1, newChild);
          i[0] = m.end();
          break;
        case '?':
          newChild = new CountedRegex(NodeType.CONCATENATION, children.get(children.size() - 1));
          newChild.min = 0;
          children.set(children.size() - 1, newChild);
          i[0]++;
          break;
        case '+':
          newChild = new CountedRegex(NodeType.CONCATENATION, children.get(children.size() - 1));
          newChild.max = KLEENE;
          children.set(children.size() - 1, newChild);
          i[0]++;
          break;
        case '(':
          i[0]++;
          children.add(new CountedRegex(s, i));
          continue;
        case ')':
          i[0]++;
          return;
        case '|':
          i[0]++;
          children = new ArrayList<>(Arrays.asList(this.copy(), new CountedRegex(s, i)));
          type = NodeType.DISJUNCTION;
          i[0]--; // Let them parse the closing ')'
          continue;
        case '*':
          i[0]++;
          newChild = new CountedRegex(NodeType.CONCATENATION, children.get(children.size() - 1));
          newChild.min = 0;
          newChild.max = KLEENE;
          children.set(children.size() - 1, newChild);
          continue;
        case '[':
          newChild = new CountedRegex(NodeType.CHARACTERRANGE);
          int endPos = s.indexOf(']', i[0]) + 1;
          newChild.value = s.substring(i[0], endPos);
          i[0] = endPos;
          children.add(newChild);
          continue;
        case '.':
          newChild = new CountedRegex(NodeType.CHARACTERRANGE);
          newChild.value = ".";
          children.add(newChild);
          i[0]++;
          continue;
        case '\\':
          i[0]++;
          if ("dwsDWS".indexOf(s.charAt(i[0])) != -1) {
            newChild = new CountedRegex(NodeType.CHARACTERRANGE);
            newChild.value = "\\" + s.charAt(i[0]);
            children.add(newChild);
            i[0]++;
            continue;
          }
          // Fall through
        default:
          newChild = new CountedRegex(NodeType.CHARACTER);
          newChild.value = "" + s.charAt(i[0]);
          children.add(newChild);
          i[0]++;
      }
    }
  }

  /** Returns a fresh copy of this regex*/
  public CountedRegex copy() {
    CountedRegex r = new CountedRegex(this.type);
    r.max = max;
    r.min = min;
    r.value = value;
    if (children != null) {
      r.children = new ArrayList<>();
      children.forEach(c -> r.children.add(c.copy()));
    }
    return (r);
  }

  /** Constructs a counted regex based on a regex */
  public CountedRegex(Regex r, Map<Regex, CountedRegex> oldToNew) {
    this.value = r.value;
    if (oldToNew != null) {
      oldToNew.put(r, this);
    }
    switch (r.type) {
      case KLEENESTAR:
        type = NodeType.CONCATENATION;
        CountedRegex c = new CountedRegex(r.children.get(0), oldToNew);
        if (c.max == 1 && c.type != NodeType.DISJUNCTION) {
          this.children = c.children;
          this.type = c.type;
          this.value = c.value;
        } else {
          this.children = new ArrayList<>(1);
          this.children.add(c);
        }
        max = KLEENE;
        min = 0;
        break;
      case EMPTY:
        type = NodeType.CONCATENATION;
        children = new ArrayList<>();
        min = max = 0;
        break;
      case CHARACTERRANGE:
        type = NodeType.CHARACTERRANGE;
        break;
      case CHARACTER:
        type = NodeType.CHARACTER;
        break;
      case DISJUNCTION:
        type = NodeType.DISJUNCTION;
        children = new ArrayList<>();
        r.children.forEach(child -> children.add(new CountedRegex(child, oldToNew)));
        break;
      case CONCATENATION:
        type = NodeType.CONCATENATION;
        children = new ArrayList<>();
        r.children.forEach(child -> children.add(new CountedRegex(child, oldToNew)));
        break;
    }
    optimize();
  }

  public CountedRegex(NodeType type) {
    this.type = type;
  }

  public CountedRegex(NodeType type, CountedRegex... children) {
    this(type);
    this.children = new ArrayList<>(Arrays.asList(children));
  }

  /** Makes the regex shorter without changing its semantics. */
  public void optimize() {
    if (children != null) {
      children.forEach(CountedRegex::optimize);
    }
    switch (this.type) {
      case CHARACTERRANGE:
        // Ranges of a single character are just single characters
        if (value.matches("\\[.\\]")) {
          type = NodeType.CHARACTER;
          value = value.substring(1, 2);
        } else if (value.matches("\\[\\\\.\\]") && "dwWsS".indexOf(value.charAt(2)) == -1) {
          type = NodeType.CHARACTER;
          value = value.substring(2, 3);
        } else if (value.matches("\\[\\\\.\\]") && "dwWsS".indexOf(value.charAt(2)) != -1) {
          value = value.substring(1, 3);
        }
        break;
      case DISJUNCTION:
        // Flatten the disjunction
        for (int i = 0; i < children.size(); i++) {
          CountedRegex child = children.get(i);
          // If the child is optional, this makes the entire disjunction optional
          if (child.min == 0) {
            this.min = 0;
            child.min = 1;
          }
          // If the child is empty, remove it
          if (child.max == 0) {
            children.remove(i);
            i--;
            continue;
          }
          // Flatten any nested concatenations
          if (child.type == NodeType.DISJUNCTION && child.min == 1 && child.max == 1) {
            children.remove(i);
            children.addAll(i, child.children);
            i--;
            continue;
          }
          // Remove it if it's already there
          for (int j = 0; j < i; j++) {
            if (children.get(j).moreGeneralThanOrEqualTo(child)) {
              children.remove(i);
              i--;
              break;
            }
          }
        }

        // Apply De Morgan
        if (children.size() > 1 && children.get(0).type == NodeType.CONCATENATION && !children.get(0).children.isEmpty() && children.get(0).max == 1
            && children.get(0).min == 1) {
          List<CountedRegex> commonStart = new ArrayList<>(children.get(0).children);
          List<CountedRegex> commonEnd = new ArrayList<>(children.get(0).children);
          for (CountedRegex child : children) {
            if (child.type == NodeType.CONCATENATION && !child.children.isEmpty() && child.max == 1 && child.min == 1) {
              int commonPrefix = 0;
              while (commonPrefix < commonStart.size() && commonPrefix < child.children.size()
                  && commonStart.get(commonPrefix).contentAndCounterEquals(child.children.get(commonPrefix))) {
                commonPrefix++;
              }
              commonStart = commonStart.subList(0, commonPrefix);
              int commonSuffix = 0;
              while (commonSuffix < commonEnd.size() && commonSuffix < child.children.size() && commonEnd.get(commonEnd.size() - commonSuffix - 1)
                  .contentAndCounterEquals(child.children.get(child.children.size() - commonSuffix - 1))) {
                commonSuffix++;
              }
              commonEnd = commonEnd.subList(commonEnd.size() - commonSuffix, commonEnd.size());
            } else {
              commonStart = Collections.emptyList();
              commonEnd = Collections.emptyList();
              break;
            }
          }
          if (!commonStart.isEmpty() || !commonEnd.isEmpty()) {
            CountedRegex disjunction = new CountedRegex(NodeType.DISJUNCTION);
            disjunction.children = children;
            for (CountedRegex child : disjunction.children) {
              child.children = child.children.subList(commonStart.size(), child.children.size() - commonEnd.size());
            }
            this.type = NodeType.CONCATENATION;
            this.children = new ArrayList<>();
            this.children.addAll(commonStart);
            this.children.add(disjunction);
            this.children.addAll(commonEnd);
            this.optimize();
            break;
          }
        }

        // Collect single characters into a range
        String rangeValue = "";
        for (int i = 0; i < children.size(); i++) {
          if (children.get(i).max != 1 || children.get(i).min != 1) {
            continue;
          }
          if (children.get(i).type == NodeType.CHARACTER) {
            if ("\\-[]^&".indexOf(children.get(i).value.charAt(0)) != -1) {
              rangeValue += '\\';
            }
            rangeValue += children.get(i).value;
            children.remove(i);
            i--;
            continue;
          }
          if (children.get(i).type == NodeType.CHARACTERRANGE) {
            if (children.get(i).value.startsWith("[")) {
              String childRange = children.get(i).value;
              if (childRange.endsWith("-]")) {
                int j = childRange.length()-2;
                while (j-- > 0 && childRange.charAt(j) == '\\') {
                }
                // escape dash if there is an even number of \ before
                if ((childRange.length() - 1 - j) % 2 == 0) {
                  childRange = childRange.substring(0, childRange.length() - 2) + "\\-]";
                }
              }
              rangeValue += childRange.substring(1, childRange.length() - 1);
            } else if (children.get(i).value.equals(".")) {
              rangeValue = "ALL";
            } else {
              rangeValue += children.get(i).value;
            }
            children.remove(i);
            i--;
          }
        }
        if (rangeValue.startsWith("ALL")) {
          children.add(new CountedRegex("."));
        } else if (!rangeValue.isEmpty()) {
          rangeValue = "[" + rangeValue + "]";
          children.add(new CountedRegex(rangeValue));
        }

        // Check whether all children are equal to the first child
        BitSet indices = new BitSet();
        boolean allEqual = true;
        for (int i = 0; i < children.size(); i++) {
          CountedRegex child = (children.get(i));
          if (child.contentEquals((children.get(0)))) {
            indices.set(child.min, child.max + 1);
          } else {
            allEqual = false;
            break;
          }
        }
        if (allEqual) {
          // Do we have a consistent range in the area over 1?
          boolean consistentRange = true;
          for (int i = indices.nextSetBit(1); i >= 0 && i < indices.length(); i++) {
            if (indices.get(i) == false) {
              consistentRange = false;
              break;
            }
          }
          // We are basically our own child
          if (consistentRange) {
            if (min == 0 && indices.get(1) || min == 1) {
              this.value = children.get(0).value;
              this.type = children.get(0).type;
              this.min = min == 0 ? 0 : indices.nextSetBit(0);
              this.max = indices.length() - 1;
              this.children = children.get(0).children;
              break;
            }
          }
        }
        // If we're only one child, collect us
        if (children.isEmpty() || this.max == 0) {
          this.children = new ArrayList<>();
          this.value = null;
          this.min = this.max = 0;
          this.type = NodeType.CONCATENATION;
        } else if (children.size() == 1 && (children.get(0).min <= 1 || this.min == this.max)) {
          this.value = children.get(0).value;
          this.type = children.get(0).type;
          this.min *= children.get(0).min;
          this.max *= children.get(0).max;
          this.children = children.get(0).children;
        }
        break;
      case CONCATENATION:
        boolean everybodyIsOptional = true;
        if (children.isEmpty()) {
          min = max = 0;
          break;
        }
        for (int i = 0; i < children.size(); i++) {
          CountedRegex child = children.get(i);
          // Flatten any nested concatenations
          if (child.type == NodeType.CONCATENATION && child.min == 1 && child.max == 1) {
            children.remove(i);
            children.addAll(i, child.children);
            i--;
            continue;
          }
          if (child.min > 0) {
            everybodyIsOptional = false;
          }
          // Group equal neighbors        
          if (i < children.size() - 1 && child.contentEquals(children.get(i + 1))) {
            child.min += children.get(i + 1).min;
            child.max += children.get(i + 1).max;
            children.remove(i + 1);
            i--;
            continue;
          }
        }
        // (a?b?)? -> (a?b?)
        if (!children.isEmpty() && everybodyIsOptional && min == 0) {
          min = 1;
        }
        lp: for (int i = 0; i < children.size(); i++) {
          CountedRegex child = (children.get(i));
          // Incorporate the next occurrence of the current conjunction
          // as in (abc){2}abc -> (abc){3}
          if (child.type == NodeType.CONCATENATION) {
            boolean isSame = true;
            sl: for (int j = 0; j < child.children.size(); j++) {
              if (i + j + 1 >= children.size() || !child.children.get(j).contentAndCounterEquals(children.get(i + j + 1))) {
                isSame = false;
                break sl;
              }
            }
            if (isSame) {
              child.min++;
              child.max++;
              for (int j = i + 1; j < child.children.size() + i + 1; j++) {
                children.remove(i + 1);
              }
              i--;
              continue;
            }
          }
          // Incorporate the previous occurrence of the current conjunction
          // as in abc(abc){2} -> (abc){3}
          if (child.type == NodeType.CONCATENATION) {
            boolean isSame = true;
            sl: for (int j = 0; j < child.children.size(); j++) {
              if (i - child.children.size() + j < 0 || !child.children.get(j).contentAndCounterEquals(children.get(i - child.children.size() + j))) {
                isSame = false;
                break sl;
              }
            }
            if (isSame) {
              child.min++;
              child.max++;
              for (int j = 0; j < child.children.size(); j++) {
                children.remove(i - 1);
                i--;
              }
              continue;
            }
          }
          // Extract beginnings from disjunctions, as in a+(a*|...) -> a+(...){0,1}
          if (child.max >= KLEENE && i + 1 < children.size() && children.get(i + 1).type == NodeType.DISJUNCTION) {
            CountedRegex disjunction = children.get(i + 1);
            for (int j = 0; j < disjunction.children.size(); j++) {
              if (disjunction.children.get(j).contentEquals(child)) {
                if (disjunction.children.get(j).min * disjunction.min <= child.min) {
                  disjunction.children.remove(j);
                  j--;
                  disjunction.min = 0;
                }
              }
            }
            if (disjunction.children.size() == 1) {
              disjunction.type = NodeType.CONCATENATION;
            }
          }
          // Group neighboring identical sequences together, as in "abcabc"
          int j = i + 1;
          while (j <= (children.size() + i) / 2 && !children.get(j).contentAndCounterEquals(children.get(i))) {
            j++;
          }
          if (j > (children.size() + i) / 2) {
            continue;
          }
          // Check whether the items between the current item and the next occurrence
          // repeat after that next occurrence
          for (int k = 1; k < j - i; k++) {
            if (!children.get(i + k).contentAndCounterEquals(children.get(j + k))) {
              continue lp;
            }
          }
          // If that is the case, group the items
          CountedRegex newChild = new CountedRegex(NodeType.CONCATENATION);
          newChild.children = new ArrayList<>(children.subList(i, j));
          newChild.min = 2;
          newChild.max = 2;
          for (int k = i + 1; k < j + (j - i); k++) {
            children.remove(i + 1);
          }
          children.set(i, newChild);
          // Make sure we treat this child again
          i--;
        }
        if (children.size() == 1 && (children.get(0).min <= 1 || this.min == this.max)) {
          this.value = children.get(0).value;
          this.type = children.get(0).type;
          this.min *= children.get(0).min;
          this.max *= children.get(0).max;
          this.children = children.get(0).children;
        }
        break;
      case CHARACTER:
        break;
    }

  }

  /** TRUE if this counted regex is more general than the other one*/
  public boolean moreGeneralThanOrEqualTo(CountedRegex other) {
    boolean contentCompatible = this.contentEquals(other);
    if (!contentCompatible && this.type == NodeType.CHARACTERRANGE && other.type == NodeType.CHARACTER && other.value.matches(this.value)) {
      contentCompatible = true;
    }
    if (!contentCompatible) {
      return (false);
    }
    return (this.min <= other.min && this.max >= other.max);
  }

  /** Creates a string of the regex */
  protected void toString(StringBuilder b) {
    int start = b.length();
    switch (type) {
      case CONCATENATION:
        if (max != 1 || min != 1) {
          b.append("(");
        }
        for (CountedRegex child : children) {
          b.append(child);
        }
        if (max != 1 || min != 1) {
          b.append(")");
        }
        break;
      case DISJUNCTION:
        b.append("(");
        for (CountedRegex child : children) {
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
    }
    if (min == 0 && max == 1) {
      b.append("?");
    } else if (min == 2 && max == 2 && b.length() - start == 1) {
      b.append(b.charAt(b.length() - 1));
    } else if (min == 2 && max == 2 && b.length() - start == 2) {
      b.append(b.charAt(b.length() - 2)).append(b.charAt(b.length() - 2));
    } else if (min == 1 && max >= KLEENE) {
      b.append("+");
    } else if (min == 0 && max >= KLEENE) {
      b.append("*");
    } else if (min == 1 && max == 1) {
      // don't do anything
    } else if (min == max) {
      b.append("{").append(max).append("}");
    } else if (max >= KLEENE) {
      b.append("{").append(min).append(",}");
    } else {
      b.append("{").append(min).append(",").append(max).append("}");
    }
  }

  /** TRUE if is empty */
  public boolean isEmpty() {
    return (max == 0);
  }

  /** TRUE if this regex equals the other regex, including the counts*/
  public boolean contentAndCounterEquals(CountedRegex c) {
    if (this.min != c.min || this.max != c.max) {
      return (false);
    }
    return (this.contentEquals(c));
  }

  /** TRUE if this regex equals the other regex irrespective of counts */
  public boolean contentEquals(CountedRegex c) {
    if (isEmpty() || c.isEmpty()) {
      return (true);
    }
    if (c.type != type) {
      return (false);
    }
    if (c.value != null && !c.value.equals(value)) {
      return (false);
    }
    if (c.children == null && this.children == null) {
      return (true);
    }
    if (c.children == null || this.children == null || this.children.size() != c.children.size()) {
      return (false);
    }
    for (int i = 0; i < this.children.size(); i++) {
      if (!children.get(i).contentAndCounterEquals(c.children.get(i))) {
        return (false);
      }
    }
    return (true);
  }

  /** Character classes (disjoint!) that we will group*/
  public static final List<String> characterClasses = Arrays.asList("[a-z]", "[A-Z]", "\\d");

  /** Generalizes the current regex */
  public void generalize() {
    switch (this.type) {
      case CHARACTERRANGE:
        if (value.startsWith("[")) {
          for (String charClass : characterClasses) {
            boolean matchesAll = true;
            for (int i = 1; i < value.length() - 1; i++) {
              if (value.charAt(i) == '-') {
                continue;
              }
              if (value.charAt(i) == '\\') {
                i++;
              }
              if (!(value.charAt(i) + "").matches(charClass)) {
                matchesAll = false;
                break;
              }
            }
            if (matchesAll) {
              value = charClass;
              break;
            }
          }
        }
        generalizeCounters();
        break;
      case CHARACTER:
        for (String charClass : characterClasses) {
          if (value.matches(charClass)) {
            type = NodeType.CHARACTERRANGE;
            value = charClass;
            break;
          }
        }
        generalizeCounters();
        break;
      case CONCATENATION:
        children.forEach(CountedRegex::generalize);
        optimize();
        generalizeCounters();
        optimize();
        break;
      case DISJUNCTION:
        // Group items that just differ in their count
        for (int i = 0; i < children.size(); i++) {
          for (int j = i + 1; j < children.size(); j++) {
            if (children.get(i).contentEquals(children.get(j))) {
              children.get(i).min = Math.min(children.get(i).min, children.get(j).min);
              children.get(i).max = Math.max(children.get(i).max, children.get(j).max);
              children.remove(j);
              j--;
            }
          }
          children.get(i).generalize();
        }
        optimize();
        generalizeCounters();
        optimize();
        break;
    }
  }

  /** Generalizes the counters of the current node and all child nodes.*/
  public void generalizeCounters() {
    if (this.max >= 2) {
      this.max = KLEENE;
      if (this.min > 1) {
        this.min = 1;
      }
    }
    if (children != null) {
      children.forEach(CountedRegex::generalizeCounters);
    }
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    toString(b);
    return b.toString();
  }

  /** Optimizes a regex*/
  public static String optimize(String regex) {
    return (new CountedRegex(regex).toString());
  }

  public static void main(String[] args) {
    CountedRegex cr = new CountedRegex(
        "[A-Z](((-P|-M\\{\\\\'|\\}|\\{\\\\'| D|\\}-H|-G|\\{\\\\| V)?[a-z])*|[A-Z]*), (-?[A-Z](\\{\\\\')?((\\}?[a-z])*|[A-Z]*)?( ?)?)+");
    cr = new CountedRegex("([|\\-]|a)");
    cr.optimize();
    System.out.println(cr);
  }

}