package com.jphaugla.config;

import com.jphaugla.service.BankService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.Executor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.jphaugla.domain.Customer;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableAsync

@ComponentScan("com.jphaugla")
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(BankService.class);
    @Autowired
    private Environment env;

    @Bean(name = "redisConnectionFactory1")
    @Primary
    public LettuceConnectionFactory redisConnectionFactory1() {
        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration();
        redisConf.setHostName(env.getProperty("spring.redis.host"));
        redisConf.setPort(Integer.parseInt(env.getProperty("spring.redis.port")));
        redisConf.setPassword(RedisPassword.of(env.getProperty("spring.redis.password")));
        return new LettuceConnectionFactory(redisConf);
    }

    @Bean
    public RedisTemplate<Object, Customer> redisTemplate1(@Qualifier("redisConnectionFactory1") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Customer> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
    @Bean
    @Primary
    public StringRedisTemplate strRedisTemplate1(@Qualifier("redisConnectionFactory1") RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean(name = "redisConnectionFactory2")
    public LettuceConnectionFactory redisConnectionFactory2() {
        RedisStandaloneConfiguration redisConf = new RedisStandaloneConfiguration();
        redisConf.setHostName(env.getProperty("spring.redis.host2"));
        redisConf.setPort(Integer.parseInt(env.getProperty("spring.redis.port2")));
        redisConf.setPassword(RedisPassword.of(env.getProperty("spring.redis.password2")));
        return new LettuceConnectionFactory(redisConf);
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate2(@Qualifier("redisConnectionFactory2") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
    @Bean
    public StringRedisTemplate strRedisTemplate2(@Qualifier("redisConnectionFactory2") RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        return redisTemplate;
    }

    @Bean("threadPoolTaskExecutor")
    public TaskExecutor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        on large 64 core machine, drove setCorePoolSize to 200 to really spike performance
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(1000);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Async-");
        return executor;
    }
}
