/*
 * Copyright 2016 Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.connect.jdbc.sink.dialect;

import org.apache.kafka.connect.data.Date;
import org.apache.kafka.connect.data.Decimal;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Time;
import org.apache.kafka.connect.data.Timestamp;

import java.util.Collection;
import java.util.Map;

import static io.confluent.connect.jdbc.sink.dialect.StringBuilderUtil.joinToBuilder;
import static io.confluent.connect.jdbc.sink.dialect.StringBuilderUtil.nCopiesToBuilder;

public class PostgreSqlDialect extends DbDialect {

  public PostgreSqlDialect() {
    super("\"", "\"");
  }

  @Override
  protected void formatColumnValue(StringBuilder builder, String schemaName, Map<String, String> schemaParameters, Schema.Type type, Object value) {
    if (schemaName != null) {
      switch (schemaName) {
        case Date.LOGICAL_NAME:
          formatTimestampFromJavaDate(builder, value).append("::date");
          return;
        case Decimal.LOGICAL_NAME:
          builder.append(value);
          return;
        case Time.LOGICAL_NAME:
          formatTimestampFromJavaDate(builder, value).append("::time");
          return;
        case Timestamp.LOGICAL_NAME:
          formatTimestampFromJavaDate(builder, value).append("::timestamp");
          return;
      }
    }
    super.formatColumnValue(builder, schemaName, schemaParameters, type, value);
  }

  private static StringBuilder formatTimestampFromJavaDate(StringBuilder builder, Object value) {
    return builder.append("to_timestamp(").append(((java.util.Date) value).getTime()).append("::double precision / 1000)");
  }

  @Override
  protected String getSqlType(String schemaName, Map<String, String> parameters, Schema.Type type) {
    if (schemaName != null) {
      switch (schemaName) {
        case Date.LOGICAL_NAME:
          return "DATE";
        case Decimal.LOGICAL_NAME:
          return "DECIMAL";
        case Time.LOGICAL_NAME:
          return "TIME";
        case Timestamp.LOGICAL_NAME:
          return "TIMESTAMP";
      }
    }
    switch (type) {
      case INT8:
        return "SMALLINT";
      case INT16:
        return "SMALLINT";
      case INT32:
        return "INT";
      case INT64:
        return "BIGINT";
      case FLOAT32:
        return "REAL";
      case FLOAT64:
        return "DOUBLE PRECISION";
      case BOOLEAN:
        return "BOOLEAN";
      case STRING:
        return "TEXT";
      case BYTES:
        return "BLOB";
    }
    return super.getSqlType(schemaName, parameters, type);
  }

  @Override
  public String getUpsertQuery(final String table, final Collection<String> keyCols, final Collection<String> cols) {
    final StringBuilder builder = new StringBuilder();
    builder.append("INSERT INTO ");
    builder.append(escaped(table));
    builder.append(" (");
    joinToBuilder(builder, ",", keyCols, cols, escaper());
    builder.append(") VALUES (");
    nCopiesToBuilder(builder, ",", "?", cols.size() + keyCols.size());
    builder.append(") ON CONFLICT (");
    joinToBuilder(builder, ",", keyCols, escaper());
    builder.append(") DO UPDATE SET ");
    joinToBuilder(
        builder,
        ",",
        cols,
        new StringBuilderUtil.Transform<String>() {
          @Override
          public void apply(StringBuilder builder, String col) {
            builder.append(escaped(col)).append("=EXCLUDED.").append(escaped(col));
          }
        }
    );
    return builder.toString();
  }

}
