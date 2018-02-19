package fr.telecom_paristech.dbweb.regexrepair.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A two dimensional array. */
public class Table<T> {

  private final static Logger log = LoggerFactory.getLogger(Table.class);

  private final int cols, rows;

  private final List<T> table;

  /** Values of cells outside of the table area */
  private final BiFunction<Integer, Integer, T> borderValueGenerator;

  /**
   * Constructor
   * @param cols number of columns
   * @param rows number of rows
   * @param value initial value
   * @param borderValueGenerator function if accessed cell lies outside of table; parameters (col, row), returns a value
   */
  public Table(int cols, int rows, T value, BiFunction<Integer, Integer, T> borderValueGenerator) {
    this.table = new ArrayList<>(cols * rows);
    for (int i = 0; i < cols * rows; i++) {
      this.table.add(value);
    }
    this.borderValueGenerator = borderValueGenerator;
    this.cols = cols;
    this.rows = rows;
  }

  /** Get value of cell */
  public T get(int col, int row) {
    if (col >= cols || row >= rows || col < 0 || row < 0) {
      if (borderValueGenerator == null) {
        return null;
      }
      return borderValueGenerator.apply(col, row);
    }
    return table.get(row * cols + col);
  }

  /** Set value of cell */
  public synchronized void set(int col, int row, T value) {
    table.set(row * cols + col, value);
  }

  /** Set multiple values of a row at once, starting at a certain column */
  public synchronized void setRowCells(int col, int row, Object... values) {
    if (cols < col + values.length) {
      log.warn("received " + values.length + " values starting from column " + col + ", but have " + cols + " columns");
      log.warn("values: " + Arrays.toString(values));
    }
    if (row >= rows) {
      log.warn("requested write on row " + row + ", but have " + rows + " rows");
    }
    for (int x = col; x < Math.min(cols, col + values.length); x++) {
      @SuppressWarnings("unchecked")
      T val = (T) values[x - col];
      table.set(row * cols + x, val);
    }
  }

  @Override
  public String toString() {
    return toString(", ", "\n");
  }

  @SuppressWarnings("rawtypes")
  public String toString(String betweenCells, String betweenLines) {
    List<StringBuilder> rowBuilder = new ArrayList<>();
    for (int row = 0; row < rows; row++) {
      rowBuilder.add(new StringBuilder());
    }
    final String skip = "__SKIP__";
    for (int col = 0; col < cols; col++) {
      int maxSubCol = 1;
      for (int subCol = 0; subCol < maxSubCol; subCol++) {
        List<String> toadd = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
          Object cell = get(col, row);
          if (cell instanceof List && ((List) cell).size() > subCol) {
            maxSubCol = Math.max(maxSubCol, ((Collection) cell).size());
            @SuppressWarnings("unchecked")
            List<T> list = ((List<T>) cell);
            toadd.add(list.get(subCol).toString());
          } else if (subCol == 0) {
            rowBuilder.get(row).append(cell.toString());
            toadd.add(skip);
          }
          else {
            toadd.add(skip);
          }
        }
        int len = toadd.stream().filter(s -> s != null && !s.equals(skip)).map(s -> s.length()).reduce(Math::max).orElse(1);
        if (len > 0) {
          for(int row = 0; row < rows; row++) {
            if (!skip.equals(toadd.get(row))) {
              rowBuilder.get(row).append(String.format("%" + len + "s", toadd.get(row)));
              if (subCol + 1 < maxSubCol) {
                rowBuilder.get(row).append(betweenCells);
              }
            }
          }
        }
      }
      int len = rowBuilder.stream().map(s -> s.length()).reduce(Math::max).orElse(1);
      for (int row = 0; row < rows; row++) {
        StringBuilder sb = rowBuilder.get(row);
        while (sb.length() < len) {
          sb.append(" ");
        }
        rowBuilder.get(row).append(betweenCells);
      }
    }

    return rowBuilder.stream().collect(Collectors.joining(betweenLines));
  }

  public int rowCount() {
    return rows;
  }

  public int colCount() {
    return cols;
  }

  /** Set all cells to a certain value */
  public void setAll(T value) {
    for (int i = 0; i < table.size(); i++) {
      table.set(i, value);
    }
  }

}