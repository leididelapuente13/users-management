package com.jcaa.usersmanagement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

  public static void main(String[] args) {
    loadDotEnvIfPresent();
    SpringApplication.run(Main.class, args);
  }

  private static void loadDotEnvIfPresent() {
    File envFile = new File(".env");
    if (!envFile.exists() || !envFile.isFile()) {
      return;
    }
    try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
          continue;
        }
        int separatorIndex = trimmed.indexOf('=');
        String key = trimmed.substring(0, separatorIndex).trim();
        String value = trimmed.substring(separatorIndex + 1).trim();
        if (!key.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
          System.setProperty(key, value);
        }
      }
    } catch (IOException ignored) {
      // Continúa con properties estándar si falla la lectura del archivo .env
    }
  }
}