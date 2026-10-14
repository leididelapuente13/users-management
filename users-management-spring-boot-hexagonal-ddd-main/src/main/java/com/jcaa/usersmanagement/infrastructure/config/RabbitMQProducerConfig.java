package com.jcaa.usersmanagement.infrastructure.config;

import java.net.URI;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
public class RabbitMQProducerConfig {

  private static final String PROP_RABBITMQ_URL =
      "${spring.rabbitmq.addresses:${RABBITMQ_URL:amqp://guest:guest@localhost:5672/}}";
  private static final String PROP_EXCHANGE =
      "${rabbitmq.exchange.user-events:user.exchange}";

  private static final String LOG_CONFIG_RABBITMQ =
      "[RabbitMQProducerConfig] Configurando conexion RabbitMQ con URL: {} y Exchange: {}";

  @Value(PROP_RABBITMQ_URL)
  private String rabbitMqUrl;

  @Value(PROP_EXCHANGE)
  private String exchangeName;

  @Bean
  public ConnectionFactory connectionFactory() {
    log.info(LOG_CONFIG_RABBITMQ, sanitizeUrl(rabbitMqUrl), exchangeName);
    final CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
    connectionFactory.setUri(rabbitMqUrl);
    return connectionFactory;
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      final ConnectionFactory connectionFactory, final MessageConverter jsonMessageConverter) {
    final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter);
    return rabbitTemplate;
  }

  @Bean
  public TopicExchange userEventsExchange() {
    return new TopicExchange(exchangeName, true, false);
  }

  @Bean
  public RabbitAdmin rabbitAdmin(final ConnectionFactory connectionFactory) {
    final RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    admin.setIgnoreDeclarationExceptions(true);
    return admin;
  }

  private String sanitizeUrl(final String url) {
    if (Objects.isNull(url)) {
      return "";
    }
    try {
      final URI uri = URI.create(url);
      final String userInfo = uri.getUserInfo();
      if (Objects.nonNull(userInfo)) {
        return url.replace(userInfo, "***:***");
      }
    } catch (final Exception ignored) {
      // Retornar url sin procesar si formato no es URI estandar
    }
    return url;
  }
}

