package com.jphaugla.config;

import com.jphaugla.domain.Customer;
import com.jphaugla.repository.RedisTemplateRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.core.IntervalFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.concurrent.Executor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableAsync

@ComponentScan("com.jphaugla")
public class RedisConfig {

    @Autowired
    private Environment env;
    private @Value("${spring.redis.timeout}")
    Duration redisCommandTimeout;

    @Bean(name = "redisConnectionFactory1")
    @Primary
    public LettuceConnectionFactory redisConnectionFactory1() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(redisCommandTimeout).build();
        RedisStandaloneConfiguration redisServerConf = new RedisStandaloneConfiguration();
        redisServerConf.setHostName(env.getProperty("spring.redis.host"));
        redisServerConf.setPort(Integer.parseInt(env.getProperty("spring.redis.port")));
        redisServerConf.setPassword(RedisPassword.of(env.getProperty("spring.redis.password")));
        return new LettuceConnectionFactory(redisServerConf,clientConfig);
    }

    // Case 3: Works
    @Bean
    public CircuitBreaker zCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry,
                                          @Value("${resilience4j.circuitbreaker.configs.default.slidingWindowSize}") Integer slidingWindowSize,
                                          @Value("${resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState}") Integer permittedNumberOfCallsInHalfOpenState,
                                          @Value("${resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls}") Integer minimumNumberOfCalls,
                                          @Value("${resilience4j.circuitbreaker.configs.default.failureRateThreshold}") Integer failureRateThreshold,
                                          //   odd error where this seems to be hardcoded to 45s somewhere
                                          // @Value("${resilience4j.circuitbreaker.configs.default.waitDurationInOpenState}") Integer waitDurationInOpenState,
                                          @Value("${resilience4j.circuitbreaker.configs.default.waitDurationInOpenStateInt}") Integer waitDurationInOpenStateInt,
                                          @Value("${resilience4j.circuitbreaker.configs.default.automaticTransitionFromOpenToHalfOpenEnabled}") Boolean automaticTransitionFromOpenToHalfOpenEnabled)

  {

        CircuitBreakerConfig cfg = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenStateInt))
                .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransitionFromOpenToHalfOpenEnabled)
                .build();

        return circuitBreakerRegistry.circuitBreaker("zCircuitBreaker", cfg);
    }

    @Bean(name = "redisConnectionFactory2")
    public LettuceConnectionFactory redisConnectionFactory2() {
        RedisStandaloneConfiguration redisServerConf2 = new RedisStandaloneConfiguration();
        redisServerConf2.setHostName(env.getProperty("spring.redis.host2"));
        redisServerConf2.setPort(Integer.parseInt("6380"));
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(redisCommandTimeout).build();
        return new LettuceConnectionFactory(redisServerConf2,clientConfig);
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplateW1(@Qualifier("redisConnectionFactory1") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate1(@Qualifier("redisConnectionFactory1") RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplateW2(@Qualifier("redisConnectionFactory2") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate2(@Qualifier("redisConnectionFactory2") RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }


    @Bean
    public RedisTemplateRepository redisTemplates(@Qualifier("redisTemplateW1") RedisTemplate redisTemplateW1, @Qualifier("redisTemplateW2") RedisTemplate redisTemplateW2,
                                                  @Qualifier("stringRedisTemplate1") StringRedisTemplate stringRedisTemplate1,  @Qualifier("stringRedisTemplate2") StringRedisTemplate stringRedisTemplate2){
        RedisTemplateRepository redisTemplateRepository = new RedisTemplateRepository( redisTemplateW1, redisTemplateW2, stringRedisTemplate1,  stringRedisTemplate2);
        return redisTemplateRepository;
    }



}
