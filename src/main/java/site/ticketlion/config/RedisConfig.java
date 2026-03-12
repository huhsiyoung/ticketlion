package site.ticketlion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    private final ObjectMapper objectMapper;

    public RedisConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());

        template.setValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));
        template.setHashValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));

        template.afterPropertiesSet();

        return template;
    }

    /**
     * 좌석 선점을 위한 Lua 스크립트
     *
     * 여러 좌석을 원자적으로 선점하는 스크립트
     * KEYS: seat:hold:{eventId}:{seatNo} 리스트
     * ARGV[1]: memberId (선점하는 사용자 ID)
     * ARGV[2]: ttlSeconds (만료 시간)
     *
     * 반환값:
     * - 1: 성공 (모든 좌석 선점 완료)
     * - 0: 실패 (하나라도 이미 선점되어 있음)
     */
    @Bean
    public DefaultRedisScript<Long> holdSeatsScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();

        // Lua 스크립트 내용
        String luaScript = """
            local memberId = ARGV[1]
            local ttl = tonumber(ARGV[2])
            
            -- 1단계: 모든 좌석이 선점 가능한지 확인
            for i, key in ipairs(KEYS) do
                local current = redis.call('GET', key)
                if current then
                    -- 이미 다른 사용자가 선점한 경우
                    if current ~= memberId then
                        return 0
                    end
                end
            end
            
            -- 2단계: 모든 좌석 선점
            for i, key in ipairs(KEYS) do
                redis.call('SETEX', key, ttl, memberId)
            end
            
            return 1
            """;

        script.setScriptText(luaScript);
        script.setResultType(Long.class);

        return script;
    }
}
