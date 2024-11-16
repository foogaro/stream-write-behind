package com.foogaro.redis.wbs.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class WBSConfiguration {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${spring.data.redis.host}") private String redis_host;
    @Value("${spring.data.redis.port}") private Integer redis_port;

    @Bean
    @ConditionalOnMissingBean(JedisConnectionFactory.class)
    public JedisConnectionFactory redisConnectionFactory() {
        logger.debug("Creating JedisConnectionFactory");
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redis_host);
        config.setPort(redis_port);
        logger.debug("Created JedisConnectionFactory");
        return new JedisConnectionFactory(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        logger.debug("Creating RedisTemplate");
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new GenericToStringSerializer<>(String.class));
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        logger.debug("Created RedisTemplate: {}", template);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        logger.debug("Creating StreamMessageListenerContainer");
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(1000))
                        .batchSize(100)
                        .build();
        StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer = StreamMessageListenerContainer.create(connectionFactory, options);
        logger.debug("Created StreamMessageListenerContainer: {}", streamMessageListenerContainer);
        return streamMessageListenerContainer;
    }

//    @Bean
//    public WBSService<?> wbsService(
//            RedisTemplate<String, String> redisTemplate,
//            ObjectMapper objectMapper) {
//        logger.debug("Creating WBSService");
//        WBSService<?> wbsService = new WBSService<>();
//        logger.debug("Created WBSService: {}", wbsService);
//        return wbsService;
//    }

}
