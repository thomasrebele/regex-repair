package fr.telecom_paristech.dbweb.regexrepair;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

public class ToolsTest {

  /** Check two lists for equality and print useful message */
  private static <T, R> void checkEquality(List<T> expected, List<R> actual) {
    org.junit.Assert.assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); i++) {
      T expItem = expected.get(i);
      R actItem = actual.get(i);
      if (!Objects.equals(expItem, actItem)) {
        org.junit.Assert.fail("lists differ at position " + i + " expected " + expItem + " but was: " + actItem);
      }
    }
  }

  @Test
  public void test() {
    List<Integer> l = Arrays.asList(1,2,3,4,5);
    List<List<Integer>> actual, expected;
    actual = Base.group(l, (x, y) -> (Math.abs(x - y) <= 2) ? x : null);
    expected = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5));
    checkEquality(expected, actual);

    actual = Base.group(l, (x, y) -> (Math.abs(x - y) <= 2) ? y : null);
    expected = Arrays.asList(Arrays.asList(1, 2, 3, 4, 5));
    checkEquality(expected, actual);
  }
}
