package org.gusdb.oauth2.eupathdb.subscriptions;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;

public class AcctDbDumpReader {

  public static void main(String[] args) {
    if (args.length != 4) {
      System.err.println("USAGE: AcctDbDumpReader <input_file> <db_connection_string> <db_user> <db_password>");
      System.exit(1);
    }
    String inputFile = args[0];
    String dbConnectionString = args[1];
    String dbUser = args[2];
    String dbPass = args[3];
    SimpleDbConfig dbConfig = SimpleDbConfig.create(SupportedPlatform.ORACLE, dbConnectionString, dbUser, dbPass);
    new AcctDbDumpReader(Paths.get(inputFile), dbConfig).loadSubscriptions();
  }

  private final Path _inputFile;
  private final SimpleDbConfig _dbConfig;
  
  public AcctDbDumpReader(Path inputFile, SimpleDbConfig dbConfig) {
    _inputFile = inputFile;
    _dbConfig = dbConfig;
  }

  private void loadSubscriptions() {
    
  }

}
