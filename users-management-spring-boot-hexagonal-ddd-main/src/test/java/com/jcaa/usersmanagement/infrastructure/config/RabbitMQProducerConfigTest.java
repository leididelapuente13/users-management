package com.jcaa.usersmanagement.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("RabbitMQProducerConfig")
class RabbitMQProducerConfigTest {

  @Test
  @DisplayName("Define beans correctamente con URL y Exchange configurados")
  void shouldDefineRabbitBeansCorrectly() {
    // Arrange
    final RabbitMQProducerConfig config = new RabbitMQProducerConfig();
    final String rabbitUrl = "amqp://user:secret@localhost:5672/test-vhost";
    final String exchangeName = "domain.events";

    ReflectionTestUtils.setField(config, "rabbitMqUrl", rabbitUrl);
    ReflectionTestUtils.setField(config, "exchangeName", exchangeName);

    // Act
    final ConnectionFactory connectionFactory = config.connectionFactory();
    final MessageConverter messageConverter = config.jsonMessageConverter();
    final RabbitTemplate rabbitTemplate = config.rabbitTemplate(connectionFactory, messageConverter);
    final TopicExchange topicExchange = config.userEventsExchange();
    final RabbitAdmin rabbitAdmin = config.rabbitAdmin(connectionFactory);

    // Assert
    assertAll(
        "RabbitMQ Producer Configuration Beans",
        () -> assertNotNull(connectionFactory, "connectionFactory must not be null"),
        () -> assertTrue(connectionFactory instanceof CachingConnectionFactory, "must be CachingConnectionFactory"),
        () -> assertNotNull(messageConverter, "messageConverter must not be null"),
        () -> assertTrue(messageConverter instanceof Jackson2JsonMessageConverter, "must be Jackson2JsonMessageConverter"),
        () -> assertNotNull(rabbitTemplate, "rabbitTemplate must not be null"),
        () -> assertEquals(messageConverter, rabbitTemplate.getMessageConverter(), "template must use JSON converter"),
        () -> assertNotNull(topicExchange, "topicExchange must not be null"),
        () -> assertEquals(exchangeName, topicExchange.getName(), "exchange name must match"),
        () -> assertNotNull(rabbitAdmin, "rabbitAdmin must not be null"));
  }
}

