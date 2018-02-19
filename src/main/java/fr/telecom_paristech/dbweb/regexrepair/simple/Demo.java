package fr.telecom_paristech.dbweb.regexrepair.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.iface.Matching;
import fr.telecom_paristech.dbweb.regexrepair.matcher.myers.MyersMatcher;

/**
 * Allows a repairer to output the current state of the tree.
 * (Used for web application that visualizes the algorithm)
 */
public class Demo {

  private static ObjectMapper mapper = new ObjectMapper();

  public static JsonStringEncoder enc = JsonStringEncoder.getInstance();

  public static class Visualization {

    public Set<Regex> origNodes, prevNodes;

    public String word;

    public List<String> steps = new ArrayList<>();

    public void setWord(String word) {
      this.word = word;
    }

    public void initTracking(Regex regex) {
      this.origNodes = regex.allNodes();
      this.prevNodes = this.origNodes;
    }

    public String allSteps() {
      StringBuilder sb = new StringBuilder();
      sb.append("[");
      sb.append(steps.stream().collect(Collectors.joining(",\n")));
      sb.append("]");
      String str = sb.toString();
      Object json;
      try {
        json = mapper.readValue(str, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
      } catch (IOException e) {
        e.printStackTrace();
        return "JACKSON EXCEPTION: " + e;
      }
    }

    public void step(Regex regex, String description) {
      Map<Regex, Integer> leafToIdx = new HashMap<>();
      StringBuilder sb = new StringBuilder();
      regexToJson(regex, sb, new AtomicInteger(), leafToIdx, origNodes, prevNodes);

      MyerMatrix m = new MyerMatrix(word, regex, new UpdateFunction());
      List<Regex> matching = m.matching();
      matching = matching.subList(1, matching.size() - 1);
      steps.add(toJson(sb.toString(), wordToJson(word.substring(1, word.length() - 1)), regexMatchingToJson(matching, leafToIdx),
          description));
      prevNodes = regex.allNodes();
    }

    public void step(String cr, String description) {
      Expr e = RegexParser.parse(cr);
      Map<Expr, Integer> leafToIdx = new HashMap<>();
      StringBuilder sb = new StringBuilder();
      regexToJson(e, sb, new AtomicInteger(), leafToIdx);

      List<Expr> matching = new MyersMatcher().match(e, word).matches;
      if (matching != null && matching.size() > 2) {
        matching = matching.subList(1, matching.size() - 1);
        steps.add(toJson(sb.toString(), wordToJson(word.substring(1, word.length() - 1)), exprMatchingToJson(matching, leafToIdx),
            description));
      }
    }

  }

  public static String wordToJson(String word) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (int i = 0; i < word.length(); i++) {
      if (i > 0) {
        sb.append(",\n");
      }
      sb.append("{ \"char\" : \"" + word.charAt(i) + "\", \"id\" : " + i + "}");
    }
    sb.append("]");
    return sb.toString();
  }

  public static String matchingToJson(Matching match, Expr root) {
    List<Expr> leaves = root.leaves();
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    int count = 0;
    for (int i = 0; i < match.matches.size(); i++) {
      if (match.matches.get(i) == null) {
        continue;
      }
      if (count++ != 0) {
        sb.append(",\n");
      }
      sb.append("{\"str\": \"");
      sb.append(i);
      sb.append("\", \"expr\": \"");
      sb.append(leaves.indexOf(match.matches.get(i)));
      sb.append("\"}");
    }
    sb.append("]");
    return sb.toString();
  }

  public static String regexMatchingToJson(List<Regex> match, Map<Regex, Integer> leafToIndex) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    int count = 0;
    for (int i = 0; i < match.size(); i++) {
      if (match.get(i) == null) {
        continue;
      }
      if (count++ != 0) {
        sb.append(",\n");
      }
      sb.append("{\"str\": \"");
      sb.append(i);
      sb.append("\", \"expr\": \"");
      sb.append(leafToIndex.get(match.get(i)));
      sb.append("\"}");
    }
    sb.append("]");
    return sb.toString();
  }

  public static String exprMatchingToJson(List<Expr> match, Map<Expr, Integer> leafToIndex) {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    int count = 0;
    for (int i = 0; i < match.size(); i++) {
      if (match.get(i) == null) {
        continue;
      }
      if (count++ != 0) {
        sb.append(",\n");
      }
      sb.append("{\"str\": \"");
      sb.append(i);
      sb.append("\", \"expr\": \"");
      sb.append(leafToIndex.get(match.get(i)));
      sb.append("\"}");
    }
    sb.append("]");
    return sb.toString();
  }

  private static void regexToJson(Expr e, StringBuilder sb, AtomicInteger ai, Map<Expr, Integer> leafToIndex) {
    sb.append("{\"name\": \"");
    sb.append(enc.quoteAsString(e.nodeText()));
    sb.append("\", ");
    if (e instanceof AggExpr) {
      sb.append("\"children\": \n[");
      boolean first = true;
      for (Expr c : ((AggExpr) e).getChildren()) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        regexToJson(c, sb, ai, leafToIndex);
      }
      sb.append("]");
    } else {
      sb.append("\"id\": ");
      int idx = ai.getAndIncrement();
      sb.append(idx);
      leafToIndex.put(e, idx);
    }
    sb.append("}\n");
  }

  private static void regexToJson(Regex e, StringBuilder sb, AtomicInteger ai, Map<Regex, Integer> leafToIndex, Set<Regex> origNodes,
      Set<Regex> prevNodes) {
    sb.append("{\"name\": \"");
    switch (e.type) {
      case CHARACTER:
      case CHARACTERRANGE:
        sb.append(enc.quoteAsString(e.value));
        break;
      case CONCATENATION:
        sb.append("&");
        break;
      case DISJUNCTION:
        sb.append("|");
        break;
      case KLEENESTAR:
        sb.append("*");
        break;
    }
    sb.append("\", ");
    if (e.children != null && e.children.size() > 0) {
      sb.append("\"children\": \n[");
      boolean first = true;
      List<Regex> cs = e.children;
      if (e.parent == null) {
        cs = cs.subList(1, cs.size() - 1);
      }
      for (Regex c : cs) {
        if (!first) {
          sb.append(",");
        }
        first = false;
        regexToJson(c, sb, ai, leafToIndex, origNodes, prevNodes);
      }
      sb.append("]");
    } else {
      sb.append("\"id\": ");
      int idx = ai.getAndIncrement();
      sb.append(idx);
      leafToIndex.put(e, idx);
    }
    sb.append(", \"state\": \"");
    if (origNodes.contains(e)) {
      sb.append("orig");
    } else if (prevNodes.contains(e)) {
      sb.append("added");
    } else {
      sb.append("new");
    }
    sb.append("\"");
    sb.append("}\n");
  }

  public static String toJson(String regex, String word, String matching, String description) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"regex\":");
    sb.append(regex);
    sb.append(",\n");
    sb.append("\"word\":");
    sb.append(word);
    sb.append(",\n");
    //
    sb.append("\"matching\":");
    sb.append(matching);
    sb.append(",\n");
    sb.append("\"description\": \"");
    sb.append(enc.quoteAsString(description));
    sb.append("\"");
    sb.append("}");
    return sb.toString();
  }

  public static void main(String[] args) {
    Visualization vis = new Visualization();
    Repair.add("a", "a(|-)", vis);
    System.out.println(vis.allSteps());
  }
}
