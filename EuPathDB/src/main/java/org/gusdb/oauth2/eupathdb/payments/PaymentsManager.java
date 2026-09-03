package org.gusdb.oauth2.eupathdb.payments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.db.runner.ParamBuilder;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.oauth2.eupathdb.AbstractDbManager;
import org.gusdb.oauth2.eupathdb.AccountDbInfo;

public class PaymentsManager extends AbstractDbManager {

  private static final String SELECT_ALL_PAYMENTS_SQL =
      "select * from " + SCHEMA_MACRO + "payments";

  private static final String SELECT_PAYMENT_BY_REF_NUM =
      SELECT_ALL_PAYMENTS_SQL + " where reference_number = ?";

  // map from JSON property name to DB column name
  private static final Map<String,String> PROPERTY_MAP = new MapBuilder<String,String>(new LinkedHashMap<>())
      .put("referenceNumber", "reference_number")
      .put("paymentDateTimeISO8601", "payment_date")
      .put("amount", "amount")
      .put("currency", "currency")
      .put("firstName", "first_name")
      .put("lastName", "last_name")
      .put("email", "email")
      .put("address1", "address1")
      .put("address2", "address2")
      .put("city", "city")
      .put("state", "state")
      .put("postalCode", "postal_code")
      .put("country", "country")
      .toMap();

  private static final String INSERT_PAYMENT_SQL =
      "insert into " + SCHEMA_MACRO + "payments (" +
      PROPERTY_MAP.values().stream().collect(Collectors.joining(", ")) +
      ") values (" +
      PROPERTY_MAP.values().stream().map(val -> "?").collect(Collectors.joining(", ")) +
      ")";

  PaymentsManager(AccountDbInfo accountDb) {
    super(accountDb);
  }

  public Optional<JsonObject> findPayment(String referenceNumber) {
    return new SQLRunner(_ds, populateSchema(SELECT_PAYMENT_BY_REF_NUM), "select-payment-by-ref-num").executeQuery(
        new ParamBuilder().addString(referenceNumber),
        rs -> rs.next() ? Optional.of(buildPaymentJson(rs)) : Optional.empty()
    );
  }

  private static JsonObject buildPaymentJson(ResultSet rs) throws SQLException {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    for (Entry<String,String> property : PROPERTY_MAP.entrySet()) {
      builder.add(property.getKey(), rs.getString(property.getValue()));
    }
    return builder.build();
  }

  public void writeAllPaymentsAsTabular(OutputStream out) {
    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {
      new SQLRunner(_ds, populateSchema(SELECT_ALL_PAYMENTS_SQL), "select-all-payments").executeQuery(rs -> {
        try {
          writer.write(PROPERTY_MAP.keySet().stream().collect(Collectors.joining(FormatUtil.TAB)));
          writer.newLine();
          while (rs.next()) {
            writer.write(PROPERTY_MAP.values().stream()
                .map(Functions.fSwallow(columnName -> rs.getString(columnName)))
                .collect(Collectors.joining(FormatUtil.TAB)));
            writer.newLine();
          }
          writer.flush();
          return null;
        }
        catch (IOException e) {
          throw new SQLRunnerException("Could not write tabular results to stream", e);
        }
      });
    }
    catch (IOException e) {
      throw new RuntimeException("Could not write tabular results to stream", e);
    }
  }

  public void insertPayment(JsonObject payment) {
    ParamBuilder params = new ParamBuilder();
    for (String propertyName : PROPERTY_MAP.keySet()) {
      String value = payment.getString(propertyName, "");
      if (value == null) {
        throw new IllegalArgumentException("Required property '" + propertyName + "' is null.");
      }
      params.addString(value);
    }
    new SQLRunner(_ds, populateSchema(INSERT_PAYMENT_SQL), "insert-payment").executeStatement(params);
  }

}
