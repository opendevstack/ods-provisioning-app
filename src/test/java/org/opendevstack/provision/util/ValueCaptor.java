package org.opendevstack.provision.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple value captor / value holder that back up values of type <em>T</em> in a list
 *
 * @param <T>
 */
public class ValueCaptor<T> {

  private final List<T> values;

  public ValueCaptor() {
    values = new ArrayList<>();
  }

  public void addValue(T value) {
    values.add(value);
  }

  public List<T> getValues() {
    return Collections.unmodifiableList(values);
  }
}
