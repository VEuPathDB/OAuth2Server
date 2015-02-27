package org.gusdb.oauth2.service;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.gusdb.oauth2.service.JerseyHttpRequestWrapper.ListEnumeration;
import org.junit.Test;

public class ListEnumerationTest {

  @Test
  public void testEnumeration() {
    List<String> items = Arrays.asList(new String[]{ "one", "two", "three" });
    Enumeration<String> enumer = new ListEnumeration<>(items);
    while (enumer.hasMoreElements()) {
      System.out.println(enumer.nextElement());
    }
  }
}
