package tech.safepay.configs;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

import java.util.concurrent.TimeUnit;

/**
 * Classe de configuração responsável por habilitar e configurar
 * o mecanismo de cache da aplicação.
 *
 * <p>
 * Utiliza o Caffeine como provider de cache em memória,
 * garantindo alta performance e baixo overhead.
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Define o {@link CacheManager} utilizado pelo Spring.
     *
     * <p>
     * O {@link CaffeineCacheManager} gerencia os caches em memória
     * e aplica as políticas configuradas via {@link Caffeine}.
     * </p>
     *
     * @return instância configurada de {@link CacheManager}
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        /*
         * Configuração do cache usando Caffeine:
         *
         * - maximumSize(10_000):
         *   Limita o cache a no máximo 10.000 entradas,
         *   evitando consumo excessivo de memória.
         *
         * - expireAfterWrite(10, TimeUnit.MINUTES):
         *   Cada item expira 10 minutos após ser gravado no cache,
         *   garantindo dados relativamente atualizados.
         *
         * - recordStats():
         *   Habilita métricas de cache (hits, misses, etc),
         *   úteis para monitoramento e tuning de performance.
         */
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
        );

        return manager;
    }
}
