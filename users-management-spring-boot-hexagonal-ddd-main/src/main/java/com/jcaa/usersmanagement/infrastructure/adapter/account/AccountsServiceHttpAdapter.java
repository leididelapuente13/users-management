package com.jcaa.usersmanagement.infrastructure.adapter.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.out.CreateUserAccountPort;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.infrastructure.adapter.account.dto.request.CreateAccountRequest;
import com.jcaa.usersmanagement.infrastructure.adapter.account.dto.response.AccountResponse;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AccountsServiceHttpAdapter implements CreateUserAccountPort {

  private static final String PROP_ACCOUNTS_SERVICE_URL =
      "${accounts.service.url:${ACCOUNTS_SERVICE_URL:https://accounts-service-ctyf.onrender.com}}";
  private static final String PROP_CONNECT_TIMEOUT_MS =
      "${accounts.service.connect-timeout-ms:5000}";
  private static final String PROP_READ_TIMEOUT_MS =
      "${accounts.service.read-timeout-ms:15000}";

  private static final String DEFAULT_CURRENCY = "COP";
  private static final String PATH_ACCOUNTS = "/api/accounts";
  private static final int MAX_ATTEMPTS = 2;
  private static final long RETRY_DELAY_MS = 1000L;

  private static final String LOG_CONFIG =
      "[AccountsServiceHttpAdapter] Configurado con base URL: {}";
  private static final String LOG_ACCOUNT_CREATED =
      "[AccountsServiceHttpAdapter] Cuenta creada exitosamente en accounts-service para userId: {} con accountId: {}";
  private static final String LOG_ACCOUNT_CONFLICT =
      "[AccountsServiceHttpAdapter] El usuario ya cuenta con una cuenta COP activa en accounts-service para userId: {}";
  private static final String LOG_ATTEMPT_FAILED =
      "[AccountsServiceHttpAdapter] Intento {}/{} fallo al llamar a accounts-service para userId: {}. Causa: {}";
  private static final String LOG_FINAL_FAILURE =
      "[AccountsServiceHttpAdapter] No se pudo crear la cuenta en accounts-service para userId: {} tras agotar {} intentos. Continuando con el registro del usuario.";
  private static final String LOG_SKIPPED =
      "[AccountsServiceHttpAdapter] ACCOUNTS_SERVICE_URL no esta configurada; omitiendo creacion de cuenta para userId: {}";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String accountsServiceUrl;

  @Autowired
  public AccountsServiceHttpAdapter(
      @Value(PROP_ACCOUNTS_SERVICE_URL) final String accountsServiceUrl,
      @Value(PROP_CONNECT_TIMEOUT_MS) final int connectTimeoutMs,
      @Value(PROP_READ_TIMEOUT_MS) final int readTimeoutMs,
      final ObjectMapper objectMapper) {
    this(
        accountsServiceUrl,
        Objects.nonNull(objectMapper) ? objectMapper : new ObjectMapper(),
        buildDefaultRestClient(accountsServiceUrl, connectTimeoutMs, readTimeoutMs));
  }

  AccountsServiceHttpAdapter(
      final String accountsServiceUrl,
      final ObjectMapper objectMapper,
      final RestClient restClient) {
    this.accountsServiceUrl = accountsServiceUrl;
    this.objectMapper = objectMapper;
    this.restClient = restClient;
    log.info(LOG_CONFIG, sanitizeUrl(accountsServiceUrl));
  }

  @Override
  public void createAccount(final UserModel user) {
    if (Objects.isNull(user) || Objects.isNull(user.getId())) {
      return;
    }

    final String userId = user.getId().value();
    if (Objects.isNull(accountsServiceUrl) || accountsServiceUrl.isBlank()) {
      log.warn(LOG_SKIPPED, userId);
      return;
    }

    final CreateAccountRequest request =
        new CreateAccountRequest(
            userId,
            DEFAULT_CURRENCY,
            user.getName().value(),
            user.getEmail().value());

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        final boolean handled = sendCreateAccountRequest(request, userId);
        if (handled) {
          return;
        }
      } catch (final Exception ex) {
        log.warn(LOG_ATTEMPT_FAILED, attempt, MAX_ATTEMPTS, userId, ex.getMessage());
        if (attempt < MAX_ATTEMPTS) {
          sleepQuietly(RETRY_DELAY_MS);
        }
      }
    }

    log.warn(LOG_FINAL_FAILURE, userId, MAX_ATTEMPTS);
  }

  private boolean sendCreateAccountRequest(
      final CreateAccountRequest request, final String userId) {
    return Boolean.TRUE.equals(
        restClient
            .post()
            .uri(PATH_ACCOUNTS)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .exchange(
                (clientRequest, clientResponse) -> {
                  final int statusCode = clientResponse.getStatusCode().value();

                  if (statusCode == HttpStatus.CREATED.value()) {
                    final AccountResponse responseBody =
                        objectMapper.readValue(clientResponse.getBody(), AccountResponse.class);
                    final String accountId =
                        Objects.nonNull(responseBody) ? responseBody.id() : "unknown";
                    log.info(LOG_ACCOUNT_CREATED, userId, accountId);
                    return true;
                  }

                  if (statusCode == HttpStatus.CONFLICT.value()) {
                    log.warn(LOG_ACCOUNT_CONFLICT, userId);
                    return true;
                  }

                  throw new IllegalStateException(
                      "accounts-service responded with HTTP status " + statusCode);
                }));
  }

  private static RestClient buildDefaultRestClient(
      final String accountsServiceUrl,
      final int connectTimeoutMs,
      final int readTimeoutMs) {
    final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    final String baseUrl = normalizeUrl(accountsServiceUrl);
    final RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
    if (!baseUrl.isBlank()) {
      builder.baseUrl(baseUrl);
    }
    return builder.build();
  }

  private static String normalizeUrl(final String url) {
    if (Objects.isNull(url)) {
      return "";
    }
    String trimmed = url.trim();
    while (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    if (trimmed.endsWith(PATH_ACCOUNTS)) {
      trimmed = trimmed.substring(0, trimmed.length() - PATH_ACCOUNTS.length());
    }
    return trimmed;
  }

  private static String sanitizeUrl(final String url) {
    if (Objects.isNull(url) || url.isBlank()) {
      return "(not configured)";
    }
    return url.replaceAll("://.*@.*", "://***:***@...");
  }

  private static void sleepQuietly(final long millis) {
    try {
      Thread.sleep(millis);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
