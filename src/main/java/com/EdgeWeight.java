package com;

public class EdgeWeight {
  private final double value;

  public EdgeWeight(double value) {
    this.value = value;
  }

  public double getValue() {
    return value;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}