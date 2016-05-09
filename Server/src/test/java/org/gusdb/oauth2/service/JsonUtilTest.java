package org.gusdb.oauth2.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;

import org.json.JSONObject;
import org.junit.Test;

public class JsonUtilTest {

  private static final String NL = System.lineSeparator();

  private static JSONObject parseOrgJson(String json) {
    return new JSONObject(json);
  }
  
  private static String prettyPrintOrgJson(JSONObject json) {
    return json.toString(2);
  }

  private static JsonObject parseJavaxJson(String json) {
    return Json.createReader(new StringReader(json)).readObject();
  }

  private static String prettyPrintJavaxJson(JsonObject json) {
    StringWriter stringWriter = new StringWriter();
    Map<String, Object> properties = new HashMap<String, Object>(1);
    properties.put(JsonGenerator.PRETTY_PRINTING, true);
    Json.createWriterFactory(properties).createWriter(stringWriter).writeObject(json);
    return stringWriter.toString();
  }

  @Test
  public void jsonPerformanceTest() {

    String orig = "{ \"stuff1\": { \"a\": 1, \"b\": true, \"c\": [] }, \"stuff2\": { \"a\": { \"b\": { \"c\": true } } } }";

    long startOrg = System.currentTimeMillis();
    JSONObject orgObj = parseOrgJson(orig);
    long printOrg = System.currentTimeMillis();
    String resultOrg = prettyPrintOrgJson(orgObj);

    long startJavax = System.currentTimeMillis();
    JsonObject javaxObj = parseJavaxJson(orig);
    long printJavax = System.currentTimeMillis();
    String resultJavax = prettyPrintJavaxJson(javaxObj);

    long end = System.currentTimeMillis();

    String result = new StringBuilder()
        .append("===== org.json =====").append(NL)
        .append("Parse: ").append(printOrg - startOrg).append("ms").append(NL)
        .append("Print: ").append(startJavax - printOrg).append("ms").append(NL)
        .append("Result: ").append(resultOrg).append(NL)
        .append(NL)
        .append("===== javax.json =====").append(NL)
        .append("Parse: ").append(printJavax - startJavax).append("ms").append(NL)
        .append("Print: ").append(end - printJavax).append("ms").append(NL)
        .append("Result: ").append(resultJavax).append(NL)
        .append(NL)
        .toString();

    System.out.println(result);
  }
}
