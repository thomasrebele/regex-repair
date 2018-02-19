package fr.telecom_paristech.dbweb.regexrepair;

import static fr.telecom_paristech.dbweb.regexrepair.RepairTests.check;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
public class ReDoSRepairTests {

  @Test
  public void test8() {
    // ReDoS attacks
    List<String> l = Arrays.asList("aaaaaaaaaaaaaaaaaaaaaaaa!");
    check("(a+)+", l);
    check("([a-zA-Z]+)*", l);
    check("(a|aa)+", l);
    check("(a|a?)+", l);
    check("(.*a){22}", l);
  }
}
