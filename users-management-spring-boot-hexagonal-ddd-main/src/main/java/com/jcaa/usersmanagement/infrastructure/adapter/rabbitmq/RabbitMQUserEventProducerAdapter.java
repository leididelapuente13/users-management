package com.jcaa.usersmanagement.infrastructure.adapter.rabbitmq;

import com.jcaa.usersmanagement.application.port.out.UserEventPublisherPort;
import com.jcaa.usersmanagement.domain.event.UserCreatedDomainEvent;
import com.jcaa.usersmanagement.domain.event.UserUpdatedDomainEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQUserEventProducerAdapter implements UserEventPublisherPort {

  private static final String PROP_EXCHANGE = "${rabbitmq.exchange.user-events:user.exchange}";
  private static final String PROP_ROUTING_KEY_CREATED =
      "${rabbitmq.routing-key.user-created:user.created}";
  private static final String PROP_ROUTING_KEY_UPDATED =
      "${rabbitmq.routing-key.user-updated:user.updated}";

  private static final String FIELD_EVENT = "event";
  private static final String FIELD_TIMESTAMP = "occurredOn";
  private static final String FIELD_PAYLOAD = "payload";

  private static final String LOG_EVENT_SENT =
      "[RabbitMQUserEventProducerAdapter] Evento {} enviado exitosamente al exchange {} con routing key {} para el usuario con id {}";
  private static final String LOG_EVENT_FAILED =
      "[RabbitMQUserEventProducerAdapter] Error al enviar evento {} al exchange {} con routing key {} para el usuario con id {}: {}";

  private final RabbitTemplate rabbitTemplate;

  @Value(PROP_EXCHANGE)
  private String exchange;

  @Value(PROP_ROUTING_KEY_CREATED)
  private String userCreatedRoutingKey;

  @Value(PROP_ROUTING_KEY_UPDATED)
  private String userUpdatedRoutingKey;

  @Override
  public void publishUserCreated(final UserCreatedDomainEvent event) {
    if (Objects.isNull(event) || Objects.isNull(event.getUser())) {
      return;
    }
    final String userId = event.getUser().getId().value();
    dispatchAsync(event.getEventName(), userCreatedRoutingKey, userId, event.payload());
  }

  @Override
  public void publishUserUpdated(final UserUpdatedDomainEvent event) {
    if (Objects.isNull(event) || Objects.isNull(event.getUser())) {
      return;
    }
    final String userId = event.getUser().getId().value();
    dispatchAsync(event.getEventName(), userUpdatedRoutingKey, userId, event.payload());
  }

  private void dispatchAsync(
      final String eventName,
      final String routingKey,
      final String userId,
      final Map<String, String> payload) {
    CompletableFuture.runAsync(
        () -> {
          try {
            final Map<String, Object> messageBody = new HashMap<>();
            messageBody.put(FIELD_EVENT, eventName);
            messageBody.put(FIELD_TIMESTAMP, Instant.now().toString());
            messageBody.put(FIELD_PAYLOAD, payload);

            rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                messageBody,
                message -> {
                  message.getMessageProperties().setCorrelationId(userId);
                  return message;
                });

            log.info(LOG_EVENT_SENT, eventName, exchange, routingKey, userId);
          } catch (final Exception exception) {
            log.warn(
                LOG_EVENT_FAILED, eventName, exchange, routingKey, userId, exception.getMessage());
          }
        });
  }
}

