package be.clinitrak.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration Redis pour le cache et les sessions.
 *
 * <p>Caches définis :
 * <ul>
 *   <li>{@code tenants} — 1h, informations des tenants actifs</li>
 *   <li>{@code users} — 15min, profils utilisateurs</li>
 *   <li>{@code roles} — 1h, liste des rôles par tenant</li>
 *   <li>{@code blacklisted-tokens} — 15min, tokens révoqués avant expiration</li>
 * </ul>
 */
@Configuration
public class RedisConfig {

    /**
     * Template Redis générique avec sérialisation JSON.
     *
     * @param factory factory de connexion Redis (auto-configurée par Spring Boot)
     * @return {@link RedisTemplate} configuré
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache manager avec TTL par cache.
     *
     * @param factory factory de connexion Redis
     * @return {@link RedisCacheManager} configuré
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
            "tenants",            defaultConfig.entryTtl(Duration.ofHours(1)),
            "users",              defaultConfig.entryTtl(Duration.ofMinutes(15)),
            "roles",              defaultConfig.entryTtl(Duration.ofHours(1)),
            "blacklisted-tokens", defaultConfig.entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
