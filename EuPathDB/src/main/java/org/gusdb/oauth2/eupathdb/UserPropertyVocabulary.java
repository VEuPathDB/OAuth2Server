package org.gusdb.oauth2.eupathdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.oauth2.assets.StaticResource;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Country code vocabulary can be produced with:
 *
 * > curl https://raw.githubusercontent.com/lukes/ISO-3166-Countries-with-Regional-Codes/refs/heads/master/slim-2/slim-2.json | jq -c "[.[] | { display: .name, value: .\"alpha-2\" }]"
 *
 * @author rdoherty
 */
public class UserPropertyVocabulary extends HashMap<String,List<String>> {

  public static final UserPropertyVocabulary VOCAB_MAP = parseVocabs();

  private static UserPropertyVocabulary parseVocabs() {
    URL vocabsUrl = new StaticResource("public/profile-vocabs.json").getResourceUrl()
        .orElseThrow(() -> new RuntimeException("Unable to find vocabulary resource in classpath."));
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(vocabsUrl.openStream()))) {
      JSONObject json = new JSONObject(IoUtil.readAllChars(reader));
      UserPropertyVocabulary vocabMap = new UserPropertyVocabulary();
      for (String key : json.keySet()) {
        JSONArray vocabObjects = json.getJSONArray(key);
        List<String> vocab = new ArrayList<>();
        for (int i = 0; i < vocabObjects.length(); i++) {
          vocab.add(vocabObjects.getJSONObject(i).getString("value"));
        }
        vocabMap.put(key, vocab);
      }
      return vocabMap;
    }
    catch (IOException e) {
      throw new RuntimeException("Can not read vocabulary JSON file.");
    }
  }

  // test method
  public static void main(String[] args) {
    for (String propName : VOCAB_MAP.keySet()) {
      System.out.println(propName + "(" + VOCAB_MAP.get(propName).size() + "): " + String.join(", ", VOCAB_MAP.get(propName)));
    }
  }
}
