package com.jcaa.usersmanagement.infrastructure.adapter.rabbitmq;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.event.UserCreatedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserUpdatedDomainEvent;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("RabbitMQUserEventProducerAdapter")
@ExtendWith(MockitoExtension.class)
class RabbitMQUserEventProducerAdapterTest {

  private static final String EXCHANGE = "user.exchange";
  private static final String ROUTING_KEY_CREATED = "user.created";
  private static final String ROUTING_KEY_UPDATED = "user.updated";
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
  @DisplayName("publishUserCreated() envia el evento a RabbitMQ de forma asincrona")
  void shouldPublishUserCreatedSuccessfully() {
    // Arrange
    final UserCreatedDomainEvent event = new UserCreatedDomainEvent(userModel);

    // Act
    assertDoesNotThrow(() -> adapter.publishUserCreated(event));

    // Assert
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_CREATED), any(Object.class), any(MessagePostProcessor.class));
  }

  @Test
  @DisplayName("publishUserUpdated() envia el evento a RabbitMQ de forma asincrona")
  void shouldPublishUserUpdatedSuccessfully() {
    // Arrange
    final UserUpdatedDomainEvent event = new UserUpdatedDomainEvent(userModel);

    // Act
    assertDoesNotThrow(() -> adapter.publishUserUpdated(event));

    // Assert
    verify(rabbitTemplate, timeout(2000))
        .convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY_UPDATED), any(Object.class), any(MessagePostProcessor.class));
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

