package com.mipt.team4.cloud_storage_backend.utils;

import java.util.Comparator;

public class NumberComparator implements Comparator<Number> {

  public static boolean equals(Number a, Number b) {
    return new NumberComparator().compare(a, b) == 0;
  }

  public static boolean lessThan(Number a, Number b) {
    return new NumberComparator().compare(a, b) < 0;
  }

  public static boolean greaterThan(Number a, Number b) {
    return new NumberComparator().compare(a, b) > 0;
  }

  public static boolean lessThanOrEqualsTo(Number a, Number b) {
    return new NumberComparator().compare(a, b) <= 0;
  }

  public static boolean greaterThanOrEqualsTo(Number a, Number b) {
    return new NumberComparator().compare(a, b) >= 0;
  }

  @Override
  public int compare(Number a, Number b) {
    if (a == null && b == null) {
      return 0;
    }
    if (a == null) {
      return -1;
    }
    if (b == null) {
      return 1;
    }

    return compareWithConversion(a, b);
  }

  private int compareWithConversion(Number a, Number b) {
    if (isIntegerType(a) && isIntegerType(b)) {
      return Long.compare(a.longValue(), b.longValue());
    }

    return Double.compare(a.doubleValue(), b.doubleValue());
  }

  private boolean isIntegerType(Number number) {
    return number instanceof Integer
        || number instanceof Long
        || number instanceof Short
        || number instanceof Byte;
  }
}
