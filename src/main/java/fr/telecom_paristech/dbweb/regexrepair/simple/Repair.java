package fr.telecom_paristech.dbweb.regexrepair.simple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.telecom_paristech.dbweb.regexrepair.iface.Feedback;
import fr.telecom_paristech.dbweb.regexrepair.simple.Demo.Visualization;
import fr.telecom_paristech.dbweb.regexrepair.simple.Regex.NodeType;

/** Repairs a regex.*/
public class Repair {

  /** Prepares  a regex for the Feedback function*/
  protected static String toString(Regex regex) {
    String s = regex.toString();
    return (s.substring(2, s.length() - 2));
  }

  /** Legacy method.*/
  public static Regex add(String word, String regexAsString, Feedback feedback) {
    return (add(word, regexAsString));
  }

  public static Regex add(String word, String regexAsString) {
    return (add(word, regexAsString, (Visualization) null));
  }
  /** Repairs a regex.*/
  public static Regex add(String word, String regexAsString, Visualization vis) {
    word = "\u27E6" + word + "\u27E7";
    Regex regex = new Regex("\u27E6(" + regexAsString + ")\u27E7");
    MyerMatrix m = new MyerMatrix(word, regex, new UpdateFunction());
    List<Regex> matching = m.matching();
    if (vis != null) {
      vis.setWord(word);
      vis.step(regexAsString, "original regex");
      vis.initTracking(regex);
      String simplified = regex.toPrettyString();
      if (simplified.length() > 2) {
        simplified = simplified.substring(1, simplified.length() - 1);
      }
      if (!regexAsString.equals(simplified)) {
        vis.step(regex, "unfolding quantifiers");
      }
    }

    // Register additions that we already did
    Map<Regex, Map<Regex, Regex>> left2right2disjunction = new HashMap<>();

    // Find all neighboring matches
    for (int i = 0; i < matching.size() - 1;) {
      int j = i + 1;
      for (; j < matching.size(); j++) {
        if (matching.get(j) != null) {
          break;
        }
      }
      // If we are at the end, we're done
      if (j == matching.size()) {
        i = j;
        continue;
      }
      // If the matches are tight neighbors, there's nothing to do
      if (j == i + 1 && matching.get(j).previous().contains(matching.get(i))) {
        i = j;
        continue;
      }

      // Find the LCA      
      List<Regex> leftParents = new ArrayList<>();
      for (Regex walker = matching.get(i); walker != null; walker = walker.parent) {
        leftParents.add(walker);
      }
      List<Regex> rightParents = new ArrayList<>();
      for (Regex walker = matching.get(j); walker != null; walker = walker.parent) {
        rightParents.add(walker);
      }
      Collections.reverse(leftParents);
      Collections.reverse(rightParents);
      int LCA = 0;
      while (LCA < leftParents.size() && LCA < rightParents.size() && leftParents.get(LCA) == rightParents.get(LCA)) {
        LCA++;
      }
      LCA--;
      if (leftParents.size() == LCA + 1) {
        LCA--;
      }
      // If we have a cross-over in a Kleene star
      if (leftParents.get(LCA + 1).childNumber >= rightParents.get(LCA + 1).childNumber) {
        // See how far we can run to the right
        // until we hit the node that is to the right
        // of the left match
        int k = j;
        while (k < word.length() && (matching.get(k) == null || !matching.get(k).previous().contains(leftParents.get(LCA + 1)))) {
          k++;
        }
        // If we found one, we can rescue the Kleene star
        if (k < word.length() && matching.get(k) != null && matching.get(k).previous().contains(leftParents.get(LCA + 1))) {
          j = k;
          // See whether we already added the same thing
          Regex disjunction = left2right2disjunction.getOrDefault(matching.get(i), Collections.emptyMap()).get(matching.get(j));
          if (disjunction != null) {
            disjunction.children.add(Regex.quoted(word.substring(i + 1, j)));
          } else {
            //disjunction = leftParents.get(LCA + 1).makeChildrenOptionalAndAdd(leftParents.get(LCA + 1).childNumber + 1,
            //    leftParents.get(LCA + 1).childNumber + 1, Regex.quoted(word.substring(i + 1, j)));
            int x = leftParents.get(LCA + 1).childNumber + 1;
            int y = leftParents.get(LCA + 1).childNumber + 1;
            //int y = matching.get(k).childNumberBelow(leftParents.get(LCA));
            x = y;
            disjunction = leftParents.get(LCA).makeChildrenOptionalAndAdd(x,
                y, Regex.quoted(word.substring(i + 1, j)));
            left2right2disjunction.putIfAbsent(matching.get(i), new HashMap<>());
            if (disjunction != null) {
              left2right2disjunction.get(matching.get(i)).put(matching.get(j), disjunction);
            }
          }
          i = j;
          continue;
        }
        // If we found no match inside the Kleene star, we ignore the match, and try again
        matching.set(j, null);
        continue;
      }

      Regex addMe = Regex.quoted(word.substring(i + 1, j));

      // Make everything optional on the left
      for (int k = LCA + 1; k < leftParents.size() - 1; k++) {
        if (leftParents.get(k).type != NodeType.CONCATENATION) {
          continue;
        }
        leftParents.get(k).makeChildrenOptionalAndAdd(leftParents.get(k + 1).childNumber + 1, Integer.MAX_VALUE, addMe);
        addMe = new Regex();
      }
      // Make everything optional on the right
      for (int k = LCA + 1; k < rightParents.size() - 1; k++) {
        if (rightParents.get(k).type != NodeType.CONCATENATION) {
          continue;
        }
        rightParents.get(k).makeChildrenOptionalAndAdd(0, rightParents.get(k + 1).childNumber, addMe);
        addMe = new Regex();
      }
      // Make the stuff in between optional  
      if (leftParents.get(LCA).type == NodeType.CONCATENATION || leftParents.get(LCA).type == NodeType.DISJUNCTION && addMe.type != NodeType.EMPTY) {
        leftParents.get(LCA).makeChildrenOptionalAndAdd(leftParents.get(LCA + 1).childNumber + 1, rightParents.get(LCA + 1).childNumber, addMe);
      }
      if (vis != null) {
        vis.step(regex, "adding disjunctions");
      }
      i = j;
    }
    // visualize simplification
    if (vis != null) {
      regex.simplify();
      Map<Regex, CountedRegex> oldToNew = new HashMap<>();
      CountedRegex cr = new CountedRegex(regex, oldToNew);
      String str = cr.toString();
      vis.step(str.substring(1, str.length() - 1), "simplified regex");
    }

    // Remove the dollar signs
    regex.children.remove(0);
    regex.children.remove(regex.children.size() - 1);
    regex.setParentPointers();
    return (regex);
  }

  public static void main(String[] args) {
    String regex = "(abc|def)";
    String word = "abbc";
    Regex expr = Repair.add(word, regex);
    System.out.println(expr.toPrettyString());
  }
}