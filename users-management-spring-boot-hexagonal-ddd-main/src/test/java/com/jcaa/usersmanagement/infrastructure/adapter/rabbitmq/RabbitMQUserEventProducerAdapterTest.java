package com.jcaa.usersmanagement.infrastructure.adapter.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.event.UserCreatedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserDeletedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserUpdatedDomainEvent;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("RabbitMQUserEventProducerAdapter")
@ExtendWith(MockitoExtension.class)
class RabbitMQUserEventProducerAdapterTest {

  private static final String EXCHANGE = "domain.events";
  private static final String ROUTING_KEY_CREATED = "user.created";
  private static final String ROUTING_KEY_UPDATED = "user.updated";
  private static final String ROUTING_KEY_DELETED = "user.deleted";
  private static final String USER_ID = "u-001";
  private static final String USER_NAME = "John Doe";
  private static final String USER_EMAIL = "john@example.com";
  private static final String PASSWORD = "password123";

  @Mock private RabbitTemplate rabbitTemplate;

  private RabbitMQUserEventProducerAdapter adapter;
  private UserModel userModel;

  @BeforeEach
  void setUp() {
    adapter = new RabbitMQUserEventProducerAdapter(rabbitTemplate);
    ReflectionTestUtils.setField(adapter, "exchange", EXCHANGE);
    ReflectionTestUtils.setField(adapter, "userCreatedRoutingKey", ROUTING_KEY_CREATED);
    ReflectionTestUtils.setField(adapter, "userUpdatedRoutingKey", ROUTING_KEY_UPDATED);
    ReflectionTestUtils.setField(adapter, "userDeletedRoutingKey", ROUTING_KEY_DELETED);

    userModel =
        new UserModel(
            new UserId(USER_ID),
            new UserName(USER_NAME),
            new UserEmail(USER_EMAIL),
            UserPassword.fromPlainText(PASSWORD),
            UserRole.MEMBER,
            UserStatus.PENDING);
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("publishUserCreated() envia contrato JSON compatible con notification-service")
  void shouldPublishUserCreatedMatchingNotificationContract() {
    // Arrange
    final UserCreatedDomainEvent event = new UserCreatedDomainEvent(userModel);

    // Act
    assertDoesNotThrow(() -> adapter.publishUserCreated(event));

    // Assert
    final ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), captor.capture(), any(MessagePostProcessor.class));

    final Map<String, Object> body = captor.getValue();
    assertAll(
        "Verificacion de contrato notification-service para user.created",
        () -> assertEquals("user.created", body.get("eventName"), "campo eventName requerido"),
        () -> assertNotNull(body.get("occurredOn"), "campo occurredOn requerido"),
        () -> assertTrue(body.get("payload") instanceof Map, "payload debe ser un objeto"),
        () -> {
          final Map<String, String> payload = (Map<String, String>) body.get("payload");
          assertEquals(USER_NAME, payload.get("name"), "campo name en payload requerido");
          assertEquals(USER_EMAIL, payload.get("email"), "campo email en payload requerido");
        });
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("publishUserUpdated() envia contrato JSON compatible con notification-service")
  void shouldPublishUserUpdatedMatchingNotificationContract() {
    // Arrange
    final UserUpdatedDomainEvent event = new UserUpdatedDomainEvent(userModel);

    // Act
    assertDoesNotThrow(() -> adapter.publishUserUpdated(event));

    // Assert
    final ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), captor.capture(), any(MessagePostProcessor.class));

    final Map<String, Object> body = captor.getValue();
    assertAll(
        "Verificacion de contrato notification-service para user.updated",
        () -> assertEquals("user.updated", body.get("eventName"), "campo eventName requerido"),
        () -> assertNotNull(body.get("occurredOn"), "campo occurredOn requerido"),
        () -> {
          final Map<String, String> payload = (Map<String, String>) body.get("payload");
          assertEquals(USER_NAME, payload.get("name"), "campo name en payload requerido");
          assertEquals(USER_EMAIL, payload.get("email"), "campo email en payload requerido");
        });
  }

  @Test
  @DisplayName("publishUserDeleted() despacha evento de eliminacion")
  void shouldPublishUserDeletedSuccessfully() {
    // Arrange
    final UserDeletedDomainEvent event = new UserDeletedDomainEvent(new UserId(USER_ID));

    // Act
    assertDoesNotThrow(() -> adapter.publishUserDeleted(event));

    // Assert
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_DELETED), any(Object.class), any(MessagePostProcessor.class));
  }

  @Test
  @DisplayName("publishUserCreated() captura AmqpException sin propagar excepcion al invocador")
  void shouldCatchAmqpExceptionGracefully() {
    // Arrange
    final UserCreatedDomainEvent event = new UserCreatedDomainEvent(userModel);
    doThrow(new AmqpException("Broker unreachable"))
        .when(rabbitTemplate)
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), any(Object.class), any(MessagePostProcessor.class));

    // Act & Assert
    assertDoesNotThrow(() -> adapter.publishUserCreated(event));
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), any(Object.class), any(MessagePostProcessor.class));
  }

  @Test
  @DisplayName("publishUserCreated() ignora evento null sin fallar ni llamar a rabbitTemplate")
  void shouldIgnoreNullEvent() {
    // Act & Assert
    assertDoesNotThrow(() -> adapter.publishUserCreated(null));
    verify(rabbitTemplate, never())
        .convertAndSend(any(), any(), any(Object.class), any(MessagePostProcessor.class));
  }
}
