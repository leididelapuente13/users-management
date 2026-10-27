package com.jcaa.usersmanagement.infrastructure.adapter.account;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("AccountsServiceHttpAdapter")
class AccountsServiceHttpAdapterTest {

  private static final String BASE_URL = "https://accounts-service-ctyf.onrender.com";
  private static final String EXPECTED_URL = "https://accounts-service-ctyf.onrender.com/api/accounts";

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockRestServiceServer mockServer;
  private AccountsServiceHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(builder).build();
    final RestClient restClient = builder.build();

    adapter = new AccountsServiceHttpAdapter(BASE_URL, objectMapper, restClient);
  }

  private UserModel buildTestUser(final String id, final String name, final String email) {
    return new UserModel(
        new UserId(id),
        new UserName(name),
        new UserEmail(email),
        UserPassword.fromPlainText("Pass1234"),
        UserRole.ADMIN,
        UserStatus.ACTIVE);
  }

  @Test
  @DisplayName("createAccount() envia POST a /api/accounts y procesa HTTP 201 Created")
  void shouldSuccessfullyCreateAccountWhenRemoteReturns201() {
    // Arrange
    final UserModel user = buildTestUser("usr-123", "Jane Doe", "jane.doe@example.com");

    final String expectedRequestBody =
        "{\"userId\":\"usr-123\",\"currency\":\"COP\",\"name\":\"Jane Doe\",\"email\":\"jane.doe@example.com\"}";

    final String responseBody =
        """
        {
          "id": "7846630f-cf78-4038-add1-c4ca25f80b11",
          "userId": "usr-123",
          "balance": 0,
          "currency": "COP",
          "status": "ACTIVE",
          "createdAt": "2026-10-27T03:00:00.000Z",
          "updatedAt": "2026-10-27T03:00:00.000Z"
        }
        """;

    mockServer
        .expect(ExpectedCount.once(), requestTo(EXPECTED_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(content().json(expectedRequestBody))
        .andRespond(withStatus(HttpStatus.CREATED).body(responseBody).contentType(MediaType.APPLICATION_JSON));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.createAccount(user));
    mockServer.verify();
  }

  @Test
  @DisplayName("createAccount() maneja HTTP 409 Conflict sin lanzar excepcion cuando ya existe la cuenta COP")
  void shouldHandleConflictGracefullyWhenUserAlreadyHasActiveAccount() {
    // Arrange
    final UserModel user = buildTestUser("usr-456", "John Doe", "john.doe@example.com");

    mockServer
        .expect(ExpectedCount.once(), requestTo(EXPECTED_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CONFLICT));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.createAccount(user));
    mockServer.verify();
  }

  @Test
  @DisplayName("createAccount() reintenta cuando el primer intento responde HTTP 500 y tiene exito en el segundo")
  void shouldRetryAndSucceedWhenFirstAttemptReturns500() {
    // Arrange
    final UserModel user = buildTestUser("usr-789", "Alice Smith", "alice@example.com");

    final String responseBody =
        """
        {
          "id": "99999999-cf78-4038-add1-c4ca25f80b11",
          "userId": "usr-789",
          "balance": 0,
          "currency": "COP",
          "status": "ACTIVE",
          "createdAt": "2026-10-27T03:00:00.000Z",
          "updatedAt": "2026-10-27T03:00:00.000Z"
        }
        """;

    mockServer
        .expect(ExpectedCount.once(), requestTo(EXPECTED_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    mockServer
        .expect(ExpectedCount.once(), requestTo(EXPECTED_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.CREATED).body(responseBody).contentType(MediaType.APPLICATION_JSON));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.createAccount(user));
    mockServer.verify();
  }

  @Test
  @DisplayName("createAccount() no tumba la creacion de usuario cuando todos los intentos fallan con HTTP 500")
  void shouldNotCrashWhenAllAttemptsFailWith500() {
    // Arrange
    final UserModel user = buildTestUser("usr-999", "Sleeping Service", "sleep@example.com");

    mockServer
        .expect(ExpectedCount.twice(), requestTo(EXPECTED_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.createAccount(user));
    mockServer.verify();
  }

  @Test
  @DisplayName("createAccount() no ejecuta llamada HTTP cuando ACCOUNTS_SERVICE_URL no esta configurada")
  void shouldSkipGracefullyWhenAccountsServiceUrlIsBlank() {
    // Arrange
    final AccountsServiceHttpAdapter blankAdapter =
        new AccountsServiceHttpAdapter("", objectMapper, RestClient.builder().build());
    final UserModel user = buildTestUser("usr-111", "No Url", "nourl@example.com");

    // Act & Assert
    assertDoesNotThrow(() -> blankAdapter.createAccount(user));
  }

  @Test
  @DisplayName("createAccount() ignora silenciosamente cuando el usuario o su id es null")
  void shouldIgnoreNullUser() {
    // Act & Assert
    assertDoesNotThrow(() -> adapter.createAccount(null));
  }

  @Test
  @DisplayName("Spring ApplicationContext puede instanciar AccountsServiceHttpAdapter sin No default constructor found")
  void shouldBeInstantiableBySpringContainer() {
    // Arrange
    try (final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.getBeanFactory().registerSingleton("objectMapper", new ObjectMapper());
      context.register(AccountsServiceHttpAdapter.class);

      // Act & Assert
      assertDoesNotThrow(context::refresh);
      final AccountsServiceHttpAdapter bean = context.getBean(AccountsServiceHttpAdapter.class);
      assertNotNull(bean);
    }
  }
}
