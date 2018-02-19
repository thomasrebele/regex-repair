package fr.telecom_paristech.dbweb.regexrepair.iface;

import java.util.List;

import fr.telecom_paristech.dbweb.regexrepair.adaptive.regex.Text;

/** Interface for an algorithm which modifies a regex in such a way, that it matches a list of given words. */
public interface RegexRepairer {

  /** Name of the repairer algorithm */
  String info();

  /** Repair a regular expression, and measure the time */
  TimedResult timedRepair(String regex, List<String> toaddList, Feedback feedback);

  /** Repair a regular expression */
  default String repair(String regex, List<String> toaddList, Feedback feedback) {
    TimedResult r = timedRepair(regex, toaddList, feedback);
    return r.expr;
  }

  /** Generate a "repaired" regex, by adding each missing word as an alternative */
  static String altBaseline(String regex, List<String> missingWords) {
    StringBuilder simpleRegex = new StringBuilder("(" + regex);
    for (int i = 0; i < missingWords.size(); i++) {
      // baseline: r' = r | w
      simpleRegex.append(")|(");
      simpleRegex.append(Text.escape(missingWords.get(i)));
    }
    simpleRegex.append(")");
    String altBaselineRegex = simpleRegex.toString();
    return altBaselineRegex;
  }
}