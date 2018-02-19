package fr.telecom_paristech.dbweb.regexrepair;

import static fr.telecom_paristech.dbweb.regexrepair.RepairTests.check;

import java.util.Arrays;

import org.junit.Test;


public class RLRepairTests {

  @Test
  public void testPhoneNumber() {
    check("\\d{3}-\\d{3}-\\d{4}", Arrays.asList("(234) 235-5678"), null);
  }

  @Test
  public void testLength() {
    check("\\d+ [mcd]?m", Arrays.asList("42 km", "34 g"), Arrays.asList("42 kmm", "42 mkm"));
  }

  @Test
  public void testVisaCC() {
    check("4\\d{15}", Arrays.asList("4000-0000-0000-0000"), null);
  }

  @Test
  public void testAmericanExpressCC() {
    check("34\\d{13}", Arrays.asList("370000000000002"), Arrays.asList("3712345678901202"));
  }

  @Test
  public void testHeadings() {
    check("<h1>.*</h1>", Arrays.asList("<h2>abc</h2>"), Arrays.asList("<h..."));
  }

  @Test
  public void testURLs() {
    check("(http.//)?(\\w+\\.)+\\w+\\.\\w+(/\\S*)?", Arrays.asList("http://www.corporate"), Arrays.asList("Monday"));

  }

}
