package fr.telecom_paristech.dbweb.regexrepair.simple.adapter;

import java.util.List;
import java.util.regex.PatternSyntaxException;

import fr.telecom_paristech.dbweb.regexrepair.helper.Tools;
import fr.telecom_paristech.dbweb.regexrepair.iface.Feedback;
import fr.telecom_paristech.dbweb.regexrepair.iface.RegexRepairer;
import fr.telecom_paristech.dbweb.regexrepair.iface.TimedResult;
import fr.telecom_paristech.dbweb.regexrepair.simple.Regex;
import fr.telecom_paristech.dbweb.regexrepair.simple.Repair;

/**
 * Implements the regex repairer interface for the algorithm of [1].
 * [1] Rebele, T., Tzompanaki, K., Suchanek, F.: Visualizing the addition of missing words to regular expressions. In: ISWC (2017)
 */
public class SimpleRepairer implements RegexRepairer {

  @Override
  public TimedResult timedRepair(String regex, List<String> toaddList, Feedback feedback) {
    TimedResult r = new TimedResult();
    r.expr = regex;
    for (String str : toaddList) {
      try {
        Regex expr = Repair.add(str, r.expr);
        String rep = expr.toPrettyString();
        r.expr = rep;
      } catch (Exception e) {
        System.err.println("problem with regex " + Tools.asJavaString(regex) + ", word " + Tools.asJavaString(str));
        if (e instanceof PatternSyntaxException) {
          System.out.println("caused by regex " + Tools.asJavaString(((PatternSyntaxException) e).getPattern()));
        }
        throw e;
      }
    }
    if (feedback == null || feedback.allow(r.expr)) {
    } else {
      r.expr = RegexRepairer.altBaseline(regex, toaddList);
    }
    return r;
  }

  @Override
  public String toString() {
    return info();
  }

  @Override
  public String info() {
    return "simple";
  }
}
