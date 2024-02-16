package org.gusdb.oauth2.service;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;

import org.gusdb.oauth2.service.util.JerseyHttpRequestWrapper.ListEnumeration;
import org.junit.Assert;
import org.junit.Test;

public class ListEnumerationTest {

  @Test
  public void testEnumeration() {
    List<String> items = Arrays.asList(new String[]{ "one", "two", "three" });
    Enumeration<String> enumer = new ListEnumeration<>(items);
    int i = 0;
    while (enumer.hasMoreElements()) {
      Assert.assertEquals(items.get(i), enumer.nextElement());
      i++;
    }
    Assert.assertEquals(items.size(), i);
  }
}
