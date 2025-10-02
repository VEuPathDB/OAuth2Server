package org.gusdb.oauth2.eupathdb;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class AccountDbInfo {

  public final DatabaseInstance DB;
  public final DataSource DATASOURCE;
  public final String SCHEMA;

  public AccountDbInfo(DatabaseInstance db, String accountDbSchema) {
    DB = db;
    SCHEMA = accountDbSchema;
    DATASOURCE = DB.getDataSource();
  }
}
