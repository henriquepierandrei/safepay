package tech.safepay.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de configuração responsável por customizar o comportamento
 * do Jackson na serialização e desserialização de JSON.
 *
 * <p>
 * Esta configuração é aplicada globalmente na aplicação Spring,
 * garantindo consistência no formato dos objetos JSON expostos
 * pelas APIs.
 * </p>
 */
@Configuration
public class JacksonConfig {

    /**
     * Cria e configura o {@link ObjectMapper} padrão da aplicação.
     *
     * <p>
     * Principais configurações:
     * </p>
     * <ul>
     *   <li>
     *     {@code findAndRegisterModules()}:
     *     Registra automaticamente módulos disponíveis no classpath,
     *     como suporte a {@code Java Time API} (LocalDate, LocalDateTime, etc).
     *   </li>
     *   <li>
     *     {@code WRITE_DATES_AS_TIMESTAMPS = false}:
     *     Desativa a serialização de datas como timestamps numéricos,
     *     utilizando o formato ISO-8601, mais legível e compatível com APIs REST.
     *   </li>
     * </ul>
     *
     * @return {@link ObjectMapper} configurado para uso global
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
