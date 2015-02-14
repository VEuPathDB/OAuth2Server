package org.gusdb.oauth2;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.gusdb.oauth2.service.ExceptionMapper;
import org.gusdb.oauth2.service.OAuthService;

public class ServiceApplication extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.add(ExceptionMapper.class);
    classes.add(OAuthService.class);
    return classes;
  }
}
