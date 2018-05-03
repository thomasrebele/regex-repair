package fr.telecom_paristech.dbweb.regexrepair.adaptive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.AggExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Alt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.ExprPos;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.RegexParser;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.AltToRep;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.CleanUp;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.CopyExpr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.EmbedInConc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.FoldRepeat;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.GeneralizeAlt;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.transform.SimplifyRepeat;
import fr.telecom_paristech.dbweb.regexrepair.data.Span;
import fr.telecom_paristech.dbweb.regexrepair.helper.RegexTools;
import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;
import fr.telecom_paristech.dbweb.regexrepair.iface.Feedback;
import fr.telecom_paristech.dbweb.regexrepair.iface.Matching;
import fr.telecom_paristech.dbweb.regexrepair.iface.RegexRepairer;
import fr.telecom_paristech.dbweb.regexrepair.iface.TimedResult;
import fr.telecom_paristech.dbweb.regexrepair.matcher.Matcher;
import fr.telecom_paristech.dbweb.regexrepair.matcher.myers.MyersMatcher;

/**
 * An algorithm to add missing words to regular expressions, checking the quality of intermediate steps.
 * Implements the algroithm of [1].
 * 
 * [1] Rebele, T., Tzompanaki, K., Suchanek, F.: Adding missing words to regular expressions. In: PAKDD (2018)
 */
public class AdaptiveRepairer implements RegexRepairer {

  /** Logger */
  public final static Logger log = LoggerFactory.getLogger(AdaptiveRepairer.class);

  /** Algorithm to approximatively match a word and a regex */
  protected Matcher matcherAlgo = new MyersMatcher();

  /** In debug mode, the algorithm does not apply the final step, in order to verify the algorithm */
  boolean debug = false;

  /** Split quantifiers that are repeated less than this number. E.g. a{10} might become a{4}a{6}, but a{10000} stays a{10000} */
  int splitRepeatUntil = 100;

  public AdaptiveRepairer() {
  }

  public AdaptiveRepairer(boolean debug) {
    this.debug = debug;
  }

  public AdaptiveRepairer(Matcher matcherAlgo) {
    this.matcherAlgo = matcherAlgo;
  }

  /** Cange splitting behavior. Modifies this instance! */
  public AdaptiveRepairer splitting(int v) {
    this.splitRepeatUntil = v;
    return this;
  }

  @Override
  public String toString() {
    return info();
  }

  @Override
  public String info() {
    return /*matcherAlgo.info() + "/" +*/ (splitRepeatUntil > 1 ? "+" : "-") + "split ";
  }

  /** Find gaps in regex tree, i.e., non consecutive "char to regex leaf" in the approximate matching.
   * This implements step 2 of algorithm described in [1] */
  protected static List<Gap> getGaps(Expr expr, String toadd, List<Expr> matching) {
    List<Gap> gaps = new ArrayList<>();
    ExprPos act = ExprPos.first(expr);
    // iterate over (c->r) in matching
    for (int i = -1; i < matching.size();) {
      int j = i + 1;
      // find next (c->r)
      for (; j < matching.size(); j++) {
        if (matching.get(j) != null && act.advance(matching.get(j)) != null) {
          break;
        }
      }

      // check whether we need to do sth
      boolean needsFix = j - i > 1;
      ExprPos next;
      if (j >= matching.size()) {
        next = ExprPos.last(expr);
      } else {
        next = act.advance(matching.get(j));
      }
      needsFix |= !ExprPos.wasSingleStep(act, next);

      // we need to do sth
      if (needsFix) {
        String toaddPart = i >= toadd.length() ? "" : toadd.substring(i + 1, j);
        gaps.add(new Gap(act, next, toaddPart, toaddPart, toadd, new HashSet<>(), new HashSet<>()));
      }
      act = next == null ? act : next;
      i = j;
    }
    return gaps;
  }

  /** Apply some cleanup and simplification steps. */
  protected static Expr postprocess(Expr e) {
    e = new CleanUp().apply(e);
    e = new SimplifyRepeat().apply(e);
    e = new FoldRepeat().apply(e);
    e = new GeneralizeAlt().apply(e);
    e = new AltToRep().apply(e);
    return e;
  }

  @Override
  public TimedResult timedRepair(String regex, List<String> toaddList, Feedback feedback) {
    TimedResult r = new TimedResult();
    Expr origExpr = (RegexParser.parse(regex));
    Expr expr = new EmbedInConc().apply(origExpr);

    // matching phase
    List<Gap> gaps = new ArrayList<>();
    r.realTimeMatchPhase = -Tools.nanos();
    for (String toadd : toaddList) {
      Matching mr = matcherAlgo.match(expr, toadd);
      gaps.addAll(getGaps(expr, mr.str, mr.matches));
    }
    r.realTimeMatchPhase += Tools.nanos();

    // fix phase
    Branch b = new Branch(null);
    createRepairAlternatives(expr, gaps, b);
    // put words to the right alternatives at the end
    addAlternatives(expr, gaps, feedback);

    // cleanup and simplify
    expr = postprocess(expr.getRoot());

    r.expr = expr.toRegexString();
    if (!debug) {
      // add those words that where fixing failed
      List<String> failed = RegexTools.countMatches(r.expr, toaddList).missed;

      if (failed.size() > 0) {
        expr = repairEvilWords(expr, failed, feedback);
        expr = postprocess(expr.getRoot());
      }
    }

    return r;
  }

  /** Add all words that could not be added with the recursive algorithm, because of negative feedback.
   * This implements line 11-17 in algorithm 3 of [1] */
  private Expr repairEvilWords(Expr orig, List<String> evilWords, Feedback f) {
    List<String> veryEvilWords = new ArrayList<>();
    List<Expr> goodExpr = new ArrayList<>();

    // generalize missing words into classes
    Map<Expr, List<String>> exprToWords = RegexTools.inferRegex(evilWords);

    // make alternative with current regex
    Alt a = new Alt();
    a.addChild(orig);

    // for every generalized class
    for (Entry<Expr, List<String>> e : exprToWords.entrySet()) {
      Expr refined = RegexParser
          .parse(RegexTools.refineRegexes(new HashSet<>(Arrays.asList(e.getKey().toRegexString())), e.getValue()).iterator().next());
      a.addChild(refined);
      String regex = a.toRegexString();

      // Check whether generalized version is acceptable
      if (f.allow(regex)) {
        goodExpr.add(refined);
      } else {
        veryEvilWords.addAll(e.getValue());
      }
      // check generalized expressions independently, to keep regex short
      a.removeLastChild();
    }

    // build the final regex
    a.addChildren(goodExpr);
    a.addChildren(veryEvilWords.stream().map(s -> Expr.textExpr(s)).collect(Collectors.toList()));

    return a;
  }

  /** Get indices of children of 'parent' between subtrees containing ep1 and ep2.
   * Exclusive, child that contains ep1 or ep2 is not contained.
   */
  private static Span getInnerSpan(AggExpr parent, ExprPos ep1, ExprPos ep2) {
    int idx1 = ExprPos.rawIndexIn(parent, ep1);
    int idx2 = ExprPos.rawIndexIn(parent, ep2);
    idx2 = idx2 > -1 ? idx2 : parent.getChildren().size();
    return new Span(idx1 + 1, idx2);
  }

  /**
   * Find gap overlaps. Modifies the expression.
   * Implements algorithm 2 of [1]
   * @param expr
   * @param gaps accumulator, filled by this method
   * @param branch
   */
  protected void createRepairAlternatives(Expr expr, List<Gap> gaps, Branch branch) {
    // stop recursion
    if (gaps.size() == 0 || !(expr instanceof AggExpr)) {
      return;
    }

    AggExpr origExpr = (AggExpr) branch.getOrigExpr(expr);
    List<Expr> oldChildren = ((AggExpr) expr).getChildren();

    if (expr instanceof Alt) {
      // just call recursively
      for (int i = 0; i < oldChildren.size(); i++) {
        createRepairAlternatives(oldChildren.get(i), filterGaps(gaps, (Alt) expr, i), branch);
      }
    }
    if (expr instanceof Conc) {
      Conc conc = (Conc) expr;

      // get inner spans
      Set<Integer> borderSet = new TreeSet<>();
      List<Span> spans = new ArrayList<>();
      for (Gap gap : gaps) {
        Span idx = getInnerSpan(origExpr, gap.leaf1, gap.leaf2);
        spans.add(idx);
        borderSet.add(idx.start);
        borderSet.add(idx.end);
      }
      borderSet.add(conc.getChildren().size());

      // iterate over every part of the concatenation
      int lastPos = 0;
      List<Expr> newChildren = new ArrayList<>();
      for (Integer actPos : borderSet) {
        int times = 2; //(actPos > lastPos ? 2 : 1);
        // we need to iterate twice for empty inner spans
        for (int x = 0; x < times; x++) {
          // concerned gaps are the gaps that cover the current part of the concatenation
          List<Gap> concernedGaps = new ArrayList<>();
          for (int i = 0; i < spans.size(); i++) {
            Span s = spans.get(i);
            // non empty gaps
            if (lastPos < actPos && s.start <= lastPos && actPos <= s.end) {
              concernedGaps.add(gaps.get(i));
            }
            // empty gaps
            if (lastPos == actPos && s.start == lastPos && s.end == s.start) {
              concernedGaps.add(gaps.get(i));
            }
          }

          // threat children of current part
          List<Expr> addTo = concernedGaps.size() > 0 ? new ArrayList<>() : newChildren;
          for (int i = lastPos; i < actPos && i < oldChildren.size(); i++) {
            List<Gap> subgaps = new ArrayList<>();
            for (int j = 0; j < gaps.size(); j++) {
              Span span = spans.get(j);
              if (span.start - 1 == i || span.end == i) {
                subgaps.add(gaps.get(j));
              }
            }
            createRepairAlternatives(oldChildren.get(i), subgaps, branch);
            addTo.add(oldChildren.get(i));
          }

          // create disjunction if necessary
          if (concernedGaps.size() > 0) {
            Alt alt = new Alt();
            if (addTo.size() == 1 && addTo.get(0) instanceof Alt) {
              alt = (Alt) addTo.get(0);
            } else {
              Conc subconc = new Conc();
              alt.addChild(subconc);
              subconc.replaceChildren(addTo);
            }
            newChildren.add(alt);
            for (Gap gap : concernedGaps) {
              if (addTo.size() > 0 || !gap.toAdd.isEmpty()) {
                gap.primaryAlts.add(alt);
              }
            }
          }

          lastPos = actPos;
        }
      }
      conc.replaceChildren(newChildren);
    } else if (expr instanceof Repeat) {
      Repeat r = (Repeat) expr;
      // check whether we need to split, e.g., a{10} becomes a{4}a{6}
      boolean doSplit = splitRepeatUntil > 1 && r.getMax() > 1 && r.getMax() < splitRepeatUntil;
      if (doSplit) {
        doSplit(r, gaps, branch);
      } else {
        // just apply the necessary modifications to the child
        // we might need to split a gap
        List<Gap> pushDown = new ArrayList<>();
        for (Gap gap : gaps) {
          Span idx = getInnerSpan(origExpr, gap.leaf1, gap.leaf2);
          if (idx.start - 1 != 0 && idx.end != 0) {
            continue;
          }
          if (gap.leaf1 != null && gap.leaf2 != null && gap.leaf1.getCycle() == gap.leaf2.getCycle()) {
            pushDown.add(gap.copy(gap.leaf1.child(), gap.leaf2.child(), gap.toAdd));
          } else {
            // check whether repeat contains gap boundaries
            // in this case we need to remove from gap end to repeat end / from repeat start to gap start
            Gap lastCreated = null;
            if (idx.end == 0 && gap.leaf2 != null) {
              pushDown.add(lastCreated = gap.copy(null, gap.leaf2.child(), ""));
            }
            if (idx.start - 1 == 0 && gap.leaf1 != null) {
              pushDown.add(lastCreated = gap.copy(gap.leaf1.child(), null, ""));
            }
            if (gap.leaf1 != null && gap.leaf1.getCycle() < r.getMin() && (gap.leaf2 == null || gap.leaf2.getRepeat() != r)) {
              pushDown.add(new Gap(null, null, "", "", gap.missingWord, gap.secondaryAlts, gap.secondaryAlts));
            }
            if (lastCreated != null) {
              lastCreated.toAdd = gap.toAdd;
            }
          }
        }

        branch = new Branch(branch.newToOld);
        createRepairAlternatives(r.getChild(), pushDown, branch);
      }
    }
  }

  /** Filter gaps by index i within parent */
  private List<Gap> filterGaps(List<Gap> gaps, AggExpr e, int idx) {
    List<Gap> subgaps = new ArrayList<>();
    for (Gap gap : gaps) {
      Span span = getInnerSpan(e, gap.leaf1, gap.leaf2);
      if (span.start - 1 == idx || span.end == idx) {
        subgaps.add(gap);
      }
    }
    return subgaps;
  }

  private int getCycle(Repeat r, ExprPos ep) {
    return ep == null || ep.getRepeat() != r ? Integer.MIN_VALUE : ep.getCycle();
  }

  /**
   * Split a repetition according to the boundaries set by the gaps
   * @param r repeat that needs splitting
   * @param gaps that affect the repeat
   * @param b accumulator
   * @param f feedback, whether we allow a change
   * @param indentStr prefix for log messages
   */
  private void doSplit(Repeat r, List<Gap> gaps, Branch b) {
    Repeat origExpr = (Repeat) b.getOrigExpr(r);
    Set<Integer> splitPos = new TreeSet<>();
    splitPos.add(r.getMax());

    // calculate split positions
    // each cycle hit by a expr pos will be put on its own
    // and whether we need to make cycles optional at the end
    // assume matches start at the left, so that we don't need removeUntil
    int removeFrom = r.getMax();
    for (Gap gap : gaps) {
      int c1 = getCycle(origExpr, gap.leaf1), c2 = getCycle(origExpr, gap.leaf2);
      splitPos.add(c1);
      splitPos.add(c2);

      // remove from
      if (c2 < 0 && c1 >= 0) {
        removeFrom = Math.min(removeFrom, c1);
      }
    }
    splitPos.remove(getCycle(origExpr, null));

    Conc conc = new Conc();
    int lastI = 0, remainingMin = r.getMin();
    for (Integer i : splitPos) {

      int inbetween = i-lastI;
      boolean isRequired = (lastI < r.getMin() || i < r.getMin());

      // add cycles in between two split positions
      Alt altBetween = new Alt();
      conc.addChild(altBetween);
      if (inbetween > 0) {
        // insert cycles not covered by any gaps
        CopyExpr cp = new CopyExpr();
        Repeat newR = (Repeat) cp.apply(r);
        altBetween.addChild(newR);

        // calculate repetition range
        newR.setMax(i == Repeat.STAR ? Repeat.STAR : inbetween);
        newR.setMin(Math.min(inbetween, remainingMin));
        remainingMin -= newR.getMin();

        // add as secondary alt to those gaps who need it
        if (i > removeFrom && isRequired) {
          for (Gap gap : gaps) {
            int c1 = getCycle(origExpr, gap.leaf1);
            if ((c1 >= 0 && i > c1)) {
              gap.secondaryAlts.add(altBetween);
            }
          }
        }
      }

      if (i >= r.getMax()) {
        break;
      }

      // create split repetitions
      CopyExpr cp = new CopyExpr();
      Repeat newR = (Repeat) cp.apply(r);
      Map<Expr, Expr> newToOld = cp.getNewToOldMap();
      b.concatenateMap(newToOld);
      newR.setMax(1);
      newR.setMin(Math.min(1, remainingMin));
      remainingMin -= newR.getMin();
      Alt altAround = new Alt();
      conc.addChild(altAround);
      altAround.addChild(newR);

      // add as secondary alt to those gaps who need it
      if (i > removeFrom && isRequired) {
        for (Gap gap : gaps) {
          int c1 = getCycle(origExpr, gap.leaf1), c2 = getCycle(origExpr, gap.leaf2);
          if ((c1 < 0 || i > c1) && (c2 < 0 || i < c2)) {
            gap.secondaryAlts.add(altAround);
          }
        }
      }

      // determine which fixes need recursive treatment
      List<Gap> pushDown = new ArrayList<>();
      for (Gap gap : gaps) {
        int c1 = getCycle(origExpr, gap.leaf1), c2 = getCycle(origExpr, gap.leaf2);
        boolean first = c1 == i, second = c2 == i;
        if (first && second) {
          pushDown.add(gap.copy(gap.leaf1.child(), gap.leaf2.child(), gap.toAdd));
        } else if (first) {
          boolean singleStep = ExprPos.wasSingleStep(gap.leaf1, null, gap.leaf1.getRepeat()); // optimization
          if (!singleStep) {
            pushDown.add(gap.copy(gap.leaf1.child(), null, ""));
          }
        } else if (second) {
          boolean singleStep = ExprPos.wasSingleStep(null, gap.leaf2, gap.leaf2.getRepeat()); // optimization
          if (!singleStep) {
            pushDown.add(gap.copy(null, gap.leaf2.child(), ""));
          }
        }
        if ((c1 == i - 1 && c2 == i)) {
          gap.primaryAlts.add(altBetween);
        }
      }

      // recursive call
      createRepairAlternatives(newR.getChild(), pushDown, new Branch(newToOld));
      lastI = i + 1;
    }

    r.getParent().substituteChild(r, conc);
  }



  /**
   * Apply the modifications required by the list of gaps.
   * @param expr
   * @param gaps
   * @param feedback
   * @return words for which fixing failed
   */
  protected List<String> addAlternatives(Expr expr, List<Gap> gaps, Feedback feedback) {
    // note: it can happen that two necessary parts will become optional
    // but checking each individually doesn't decrease performance
    // e.g. a.*b

    // check for unchangeable secondary alts
    Set<Alt> unchangeable = new HashSet<>();
    Set<String> evilWords = new HashSet<>();
    for (Gap g : gaps) {
      if (evilWords.contains(g.missingWord)) {
        continue;
      }
      unchangeable.addAll(getUnchangeable(g.secondaryAlts, feedback));
      if (!Collections.disjoint(unchangeable, g.secondaryAlts)) {
        evilWords.add(g.missingWord);
      }
    }
    gaps.removeIf(gap -> evilWords.contains(gap.missingWord));

    // check for unchangeable primary alts
    for (Gap gap : gaps) {
      // if we have exactly one unchangeable alt, we can add the missing part to it, if it's not empty
      if (gap.primaryAlts.size() > 1) {
        gap.primaryAlts.removeAll(getUnchangeable(gap.primaryAlts, feedback));
      }

      // check whether we can add the missing part
      if (!gap.originalToAdd.isEmpty() && gap.primaryAlts.size() == 0) {
        evilWords.add(gap.missingWord);
      }
    }
    gaps.removeIf(gap -> evilWords.contains(gap.missingWord));

    Map<String, List<Gap>> missingWordToGaps = new LinkedHashMap<>();
    for (Gap gap : gaps) {
      missingWordToGaps.computeIfAbsent(gap.missingWord, k -> new ArrayList<>()).add(gap);
    }

    // try to repair word by word
    for (String missingWord : missingWordToGaps.keySet()) {
      // memorize expressions added to alternatives, used to remove it later
      Map<Alt, List<Expr>> altToNewExpr = new HashMap<>();
      List<Gap> fs = missingWordToGaps.get(missingWord);
      for (Gap gap : fs) {
        gap.secondaryAlts.forEach(alt -> addToAlt(alt, new Conc(), altToNewExpr));

        // search candidate, prefer alts closer to root
        if (gap.primaryAlts.size() > 0) {
          Alt best = null;
          for (Alt alt : gap.primaryAlts) {
            if (best == null || alt.depth() < best.depth()) {
              best = alt;
            }
          }

          addToAlt(best, Expr.textExpr(gap.toAdd), altToNewExpr);
          if (best.getChildren().size() == 1) {
            addToAlt(best, new Conc(), altToNewExpr);
          }

          Alt tmp = best;
          gap.primaryAlts.forEach(a -> {
            if (a != tmp) {
              addToAlt(a, new Conc(), altToNewExpr);
            }
          });
        }
      }

      Expr e = postprocess(expr.getRoot()); // don't use expr = .... here, 
      boolean allow = feedback == null || feedback.allow(e.toRegexString());

      if (!allow) {
        evilWords.add(missingWord);
        // undo changes
        for (Alt a : altToNewExpr.keySet()) {
          List<Expr> children = new ArrayList<>(a.getChildren());
          children.removeAll(altToNewExpr.get(a));
          a.replaceChildren(children);
        }
      }
    }

    return new ArrayList<>(evilWords);

  }

  /** Get a list of "unchangeable" disjunction nodes.
   * Adding an empty alternative to those nodes leads to a bad regex, so it should be avoided. */
  private Set<Alt> getUnchangeable(Set<Alt> alts, Feedback feedback) {
    Set<Alt> unchangeable = new HashSet<>();
    if (feedback != null) {
      for (Alt alt : alts) {
        if (alt != null) {
          alt.addChild(new Conc());
        }
        Expr e = postprocess(alt.getRoot());
        if (!feedback.allow(e.toRegexString())) {
          unchangeable.add(alt);
        }
        // undo
        alt.removeLastChild();
      }
    }
    return unchangeable;
  }

  /** Add e as alternative to a, tracking changes */
  private void addToAlt(Alt a, Expr e, Map<Alt, List<Expr>> changed) {
    // if e is empty, only add if necessary
    if (e instanceof Conc && ((AggExpr) e).getChildren().size() == 0) {
      if (a.acceptsEmptyWord()) {
        return;
      }
    }
    a.addChild(e);
    changed.computeIfAbsent(a, k -> new ArrayList<>(1)).add(e);
  }

}
