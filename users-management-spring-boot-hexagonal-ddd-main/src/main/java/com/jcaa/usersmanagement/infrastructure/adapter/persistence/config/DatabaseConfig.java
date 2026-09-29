package com.jcaa.usersmanagement.infrastructure.adapter.persistence.config;

import java.util.Objects;

public record DatabaseConfig(
    String type,
    String host,
    int port,
    String databaseName,
    String username,
    String password,
    String url) {

  private static final String MYSQL_URL_TEMPLATE =
      "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
  private static final String POSTGRES_URL_TEMPLATE =
      "jdbc:postgresql://%s:%d/%s";
  private static final String TYPE_POSTGRES = "postgres";
  private static final String TYPE_POSTGRESQL = "postgresql";
  private static final String PREFIX_POSTGRES = "postgres://";
  private static final String PREFIX_POSTGRESQL = "postgresql://";
  private static final String PREFIX_JDBC = "jdbc:";

  public DatabaseConfig(
      final String host,
      final int port,
      final String databaseName,
      final String username,
      final String password) {
    this("mysql", host, port, databaseName, username, password, null);
  }

  public String buildJdbcUrl() {
    if (Objects.nonNull(url) && !url.isBlank()) {
      final String trimmedUrl = url.trim();
      if (trimmedUrl.startsWith(PREFIX_POSTGRES) || trimmedUrl.startsWith(PREFIX_POSTGRESQL)) {
        return PREFIX_JDBC + trimmedUrl;
      }
      return trimmedUrl;
    }
    if (Objects.nonNull(type)
        && (TYPE_POSTGRES.equalsIgnoreCase(type) || TYPE_POSTGRESQL.equalsIgnoreCase(type))) {
      return String.format(POSTGRES_URL_TEMPLATE, host, port, databaseName);
    }
    return String.format(MYSQL_URL_TEMPLATE, host, port, databaseName);
  }
}
