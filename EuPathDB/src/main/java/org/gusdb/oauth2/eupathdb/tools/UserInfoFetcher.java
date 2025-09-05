package org.gusdb.oauth2.eupathdb.tools;

import org.gusdb.oauth2.client.KeyStoreTrustManager;
import org.gusdb.oauth2.client.OAuthClient;
import org.gusdb.oauth2.client.OAuthConfig;
import org.gusdb.oauth2.client.ValidatedToken;
import org.gusdb.oauth2.client.veupathdb.UserInfoImpl;
import org.gusdb.oauth2.client.veupathdb.UserImpl;
import org.gusdb.oauth2.client.veupathdb.UserInfo;
import org.gusdb.oauth2.exception.ExpiredTokenException;
import org.gusdb.oauth2.exception.InvalidTokenException;
import org.json.JSONObject;

public class UserInfoFetcher extends ToolBase {

  private static final String PROP_APICOMPONENTSITE_SECRET = "apiComponentSiteSecret";
  private static final String PROP_TEST_BEARER_TOKEN = "testBearerToken";

  public static void main(String[] args) throws InvalidTokenException, ExpiredTokenException {
    new UserInfoFetcher(args).execute();
  }

  public UserInfoFetcher(String[] args) {
    super(args, new String[] { PROP_APICOMPONENTSITE_SECRET, PROP_TEST_BEARER_TOKEN });
  }

  public void execute() throws InvalidTokenException, ExpiredTokenException {

    String apiComponentSiteSecret = findProp(PROP_APICOMPONENTSITE_SECRET);
    String tokenValue = findProp(PROP_TEST_BEARER_TOKEN);

    OAuthConfig oauthConfig = OAuthConfig.build(
        "https://eupathdb.org/oauth",
        "apiComponentSite",
        apiComponentSiteSecret
    );

    OAuthClient client = new OAuthClient(new KeyStoreTrustManager());

    ValidatedToken token = client.getValidatedEcdsaSignedToken(oauthConfig.getOauthUrl(), tokenValue);

    //ValidatedToken token = ValidatedToken.build(TokenType.BEARER, tokenValue, null);

    String sentHeaderValue = OAuthClient.getAuthorizationHeaderValue(token);
    System.out.println("Passing header value: " + sentHeaderValue);

    checkValue(sentHeaderValue);

    // confirm parsing
    String receivedToken = OAuthClient.getTokenFromAuthHeader(sentHeaderValue);
    System.out.println("Parsed raw token: " + receivedToken);

    // first try BasicUser method
    JSONObject json = client.getUserData(oauthConfig.getOauthUrl(), token);
    System.out.println("Raw Token Data:\n" + json.toString(2));
    System.out.println(new UserInfoImpl(json).getDisplayName());

    // second try BearerTokenUser method
    UserInfo user = new UserImpl(client, oauthConfig.getOauthUrl(), token);
    System.out.println(user.getDisplayName());
  }

  private static void checkValue(String value) {
    char LF = '\n';
    int index = value.indexOf(LF);
    while (index != -1) {
        index++;
        if (index < value.length()) {
            char c = value.charAt(index);
            if ((c==' ') || (c=='\t')) {
                // ok, check the next occurrence
                index = value.indexOf(LF, index);
                continue;
            }
        }
        throw new IllegalArgumentException(
            "Illegal character(s) in message header value: " + value);
    }
  }
}
