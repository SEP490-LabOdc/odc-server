package com.odc.commonlib.event;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.MethodKafkaListenerEndpoint;
import org.springframework.messaging.handler.annotation.support.DefaultMessageHandlerMethodFactory;
import org.springframework.messaging.handler.annotation.support.MessageHandlerMethodFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Configuration
public class EventConsumerConfig {
    private static final Logger logger = LoggerFactory.getLogger(EventConsumerConfig.class);

    private final ApplicationContext applicationContext;
    private final ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory;
    private final KafkaListenerEndpointRegistry endpointRegistry;

    public EventConsumerConfig(ApplicationContext applicationContext,
                               ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory,
                               @Qualifier("kafkaListenerEndpointRegistry") KafkaListenerEndpointRegistry endpointRegistry) {
        this.applicationContext = applicationContext;
        this.kafkaListenerContainerFactory = kafkaListenerContainerFactory;
        this.endpointRegistry = endpointRegistry;
    }

    @PostConstruct
    public void registerEventHandlers() {
        logger.info("Scanning for and registering custom EventHandlers...");
        applicationContext.getBeansOfType(EventHandler.class)
                .forEach(this::registerConsumer);
    }

    private void registerConsumer(String beanName, EventHandler handler) {
        String topic = handler.getTopic();
        Method handleMethod = ReflectionUtils.findMethod(handler.getClass(), "handle", byte[].class);

        Assert.notNull(handleMethod, "Method 'handle(byte[])' not found on handler: " + beanName);
        Assert.hasText(topic, "Topic must not be empty or null for handler: " + beanName);

        MethodKafkaListenerEndpoint<String, byte[]> endpoint = new MethodKafkaListenerEndpoint<>();

        endpoint.setId(beanName + "-" + topic);
        endpoint.setTopics(topic);
        endpoint.setBean(handler);
        endpoint.setMethod(handleMethod);
        endpoint.setMessageHandlerMethodFactory(messageHandlerMethodFactory());

        endpointRegistry.registerListenerContainer(endpoint, kafkaListenerContainerFactory, true);
        logger.info("Successfully registered Kafka consumer for topic '{}' with handler '{}'", topic, beanName);
    }

    private MessageHandlerMethodFactory messageHandlerMethodFactory() {
        return new DefaultMessageHandlerMethodFactory();
    }
}