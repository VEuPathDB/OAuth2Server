package org.gusdb.oauth2.eupathdb;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.platform.DBPlatform;

public abstract class AbstractDbManager {

  protected static final String SCHEMA_MACRO = "$$accountschema$$";

  protected final DataSource _ds;

  private final DBPlatform _platform;
  private final String _schema;

  protected AbstractDbManager(AccountDbInfo accountDb) {
    _ds = accountDb.DATASOURCE;
    _platform = accountDb.DB.getPlatform();
    _schema = accountDb.SCHEMA;
  }

  protected String populateSchema(String sql) {
    return sql.replace(SCHEMA_MACRO, _schema);
  }

  protected long nextIdFromSequence(String tableName) {
    try {
      return _platform.getNextId(_ds, _schema, tableName);
    }
    catch (SQLException e) {
      throw new RuntimeException("Could not retrieve next ID for table " + tableName);
    }
  }
}
