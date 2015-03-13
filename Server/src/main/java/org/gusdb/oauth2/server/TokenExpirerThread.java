package org.gusdb.oauth2.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.service.TokenStore;

public class TokenExpirerThread implements Runnable {

  private static final long THREAD_SLEEP_SECS = 20;

  private static final ScheduledExecutorService _executor =
      Executors.newSingleThreadScheduledExecutor();

  public static void start(ApplicationConfig config) {
    _executor.scheduleAtFixedRate(
        new TokenExpirerThread(config.getTokenExpirationSecs()),
        THREAD_SLEEP_SECS, THREAD_SLEEP_SECS, TimeUnit.SECONDS);
  }

  public static void shutdown() {
    _executor.shutdown();
  }

  private int _tokenExpirationSecs;
  
  public TokenExpirerThread(int tokenExpirationSecs) {
    _tokenExpirationSecs = tokenExpirationSecs;
  }
  
  @Override
  public void run() {
    TokenStore.removeExpiredTokens(_tokenExpirationSecs);
  }
}
