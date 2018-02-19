package fr.telecom_paristech.dbweb.regexrepair.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.CharacterClass;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Conc;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Expr;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Repeat;
import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Text;

/** Various functions dealing with strings and regexes */
public class RegexTools {

  private final static Logger LOG = LoggerFactory.getLogger(RegexTools.class);

  /** Pattern for replacing \W or \W{...} with sth else */
  public final static java.util.regex.Pattern WCLASSREPEAT_REPLACE = java.util.regex.Pattern.compile("(\\\\W(\\{[^}]*\\})?)");

  /** Pattern for replacing \W with sth else */
  public final static java.util.regex.Pattern WCLASS_REPLACE = java.util.regex.Pattern.compile("\\\\W");

  /** Class for result of countMatches(...) */
  public static class MatchCount {

    public List<String> matched = new ArrayList<>();

    public List<String> missed = new ArrayList<>();
  }

  /** Apply regex to list, obtaining matched/non-matched strings */
  public static MatchCount countMatches(String r, List<String> list) {
    MatchCount result = new MatchCount();
    com.google.re2j.Pattern pattern = com.google.re2j.Pattern.compile(r);
    long diff = -Tools.nanos();
    for (String t : list) {
      if (pattern.matcher(t).matches()) {
        result.matched.add(t);
      } else {
        result.missed.add(t);
      }
    }
    diff += Tools.nanos();
    if (diff > 10 * (1e6 /*ms*/)) {
      LOG.debug("counting matches for {}, with list {}", Tools.asJavaString(r), Tools.asJavaString(list));
    } else {
      if (LOG.isTraceEnabled()) {
        LOG.trace("counting matches for {}, with list {}", Tools.asJavaString(r), Tools.asJavaString(list));
      }
    }
    return result;
  }

  /** Transform a word to a regex by replacing a,..., z by [a-z], similar for other characters, and grouping them with quantifiers.
   * Example: abc12 becomes [a-z]{3}\d{2}
   */
  public static Expr regexForWord(String word) {
    Conc result = new Conc();
    Expr last = new Conc();
    int count = 0;
    for (int i = 0; i < word.length(); i++) {
      Expr act = null;
      char c = word.charAt(i);
      if ('A' <= c && c <= 'Z') {
        act = new CharacterClass("[A-Z]");
      } else if ('a' <= c && c <= 'z') {
        act = new CharacterClass("[a-z]");
      } else if ('0' <= c && c <= '9') {
        act = new Text("\\d", false);
      } else if ('_' == c) {
        act = new Text("\\w", false);
      } else {
        //r.addChild(new Text("" + c));
        act = new Text("\\W", false);
      }
      if (Objects.equals(last.toRegexString(), act.toRegexString())) {
        count++;
      } else {
        if (count > 0) {
          result.addChild(new Repeat(last, count, count));
        }
        count = 1;
      }
      last = act;
    }
    if (count > 0) {
      result.addChild(new Repeat(last, count, count));
    }
    return result;
  }

  /**
   * Takes output of regexForWord(...) and returns a regex containing only character classes
   * Example: [a-z]{3}\d{2} becomes [a-z]\d
   */
  public static String structure(Expr expr) {
    return expr.transform(e -> {
      if (e instanceof Repeat) {
        return ((Repeat) e).getChildren().get(0);
      }
      return e;
    }).toRegexString();
  }

  /** Replace matches in string by applying a function on each match */
  public static String replace(java.util.regex.Pattern p, String str, BiFunction<Integer, java.util.regex.Matcher, String> fn, int[] count) {
    java.util.regex.Matcher m = p.matcher(str);
    StringBuffer sb = new StringBuffer();
    count[0] = 0;
    while (m.find()) {
      m.appendReplacement(sb, fn.apply(count[0]++, m).replace("\\", "\\\\").replace("$", "\\$"));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /** Refine regexes by replacing \W with a character class containing only actually used characters */
  public static Set<String> refineRegexes(Set<String> regexes, List<String> words) {
    List<String> tmpRegexes = new ArrayList<>(regexes);
    List<List<java.util.regex.Matcher>> matches = new ArrayList<>();
    for (int i = 0; i < tmpRegexes.size(); i++) {
      String regex = tmpRegexes.get(i);

      // find which characters match \W
      int count[] = {0};
      String tmpRegex = replace(RegexTools.WCLASSREPEAT_REPLACE, regex, (j, y) -> "(?<g" + (j++) + ">" + y.group() + ")", count);
      java.util.regex.Pattern p = java.util.regex.Pattern.compile(tmpRegex);
      matches.add(words.stream().map(s -> p.matcher(s)).filter(m -> m.matches()).collect(Collectors.toList()));
      if (matches.get(i).size() == 0) {
        continue;
      }

      List<Set<String>> found = new ArrayList<>(count[0]);
      for (int j = 0; j < count[0]; j++) {
        HashSet<String> set = new HashSet<>();
        found.add(set);
        for (java.util.regex.Matcher m : matches.get(i)) {
          set.add(m.group("g" + j));
        }
      }
      String newRegex = replace(RegexTools.WCLASS_REPLACE, regex,
          (j, y) -> CharacterClass.from(found.get(j).stream().collect(Collectors.joining())),
          new int[] { 0 });
      tmpRegexes.set(i, newRegex);
    }

    return new HashSet<>(tmpRegexes);
  }

  /** 
   * Create generalized regexes from a list of words.
   * Based on parst of the algorithm of [2].
   * [2] Babbar, R., Singh, N.: Clustering based approach to learning regular expressions over large alphabet for noisy unstructured text. In: Workshop on Analytics for Noisy Unstructured Text Data (2010)
   * @return mapping from regex to words
   */
  public static Map<Expr, List<String>> inferRegex(List<String> words) {
    // create very basic regexes, e.g., map word abc12 to regex [a-z]{3}\d{2}
    Map<String, Expr> wordToRegex = words.stream().collect(Collectors.toMap(Function.identity(), s -> regexForWord(s)));
    // get the gist of the regex, e.g., [a-z]{3}\d{2} becomes [a-z]\d
    // map [a-z]\d to list containing abc12
    Map<String, List<String>> structureToWords = words.stream() //
        .collect(Collectors.groupingBy(s -> structure(wordToRegex.get(s))));
    // result of generalized regex to list of words that lead to this regex
    Map<Expr, List<String>> result = new HashMap<>();

    for (String struct : structureToWords.keySet()) {
      List<String> wordsForStructure = structureToWords.get(struct);
      List<Expr> regexesForWords = wordsForStructure.stream().map(s -> wordToRegex.get(s)).collect(Collectors.toList());

      // combine regexesForWords by adapting ranges of quantifiers
      // example: combine a{2}b{4} and a{1}b{10} to a{1,2}b{4,10}
      List<Expr> parts = new ArrayList<>();
      for (Expr e : regexesForWords) {
        List<Expr> cs = ((Conc) e).getChildren();
        if (parts.size() == 0) {
          parts.addAll(cs);
          continue;
        }
        for (int i = 0; i < cs.size(); i++) {
          Repeat base = (Repeat) parts.get(i);
          Repeat other = (Repeat) cs.get(i);
          base.setMax(Math.max(base.getMax(), other.getMax()));
          base.setMin(Math.min(base.getMin(), other.getMin()));
        }
      }
      result.put(new Conc(parts), wordsForStructure);
    }

    return result;
  }


}
