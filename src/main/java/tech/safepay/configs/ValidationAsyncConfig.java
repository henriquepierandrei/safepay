package tech.safepay.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Classe de configuração responsável por definir o executor
 * utilizado para execuções assíncronas relacionadas a validações.
 *
 * <p>
 * Centraliza a política de concorrência para tarefas que não
 * devem bloquear o fluxo principal da aplicação.
 * </p>
 */
@Configuration
public class ValidationAsyncConfig {

    /**
     * Executor dedicado para tarefas de validação assíncrona.
     *
     * <p>
     * Utiliza um {@code FixedThreadPool} com quantidade de threads
     * baseada no número de processadores disponíveis,
     * garantindo bom equilíbrio entre paralelismo e consumo de recursos.
     * </p>
     *
     * <p>
     * Este executor pode ser injetado via {@code @Qualifier("validationExecutor")}
     * ou utilizado em métodos anotados com {@code @Async("validationExecutor")}.
     * </p>
     *
     * @return {@link Executor} configurado para validações
     */
    @Bean(name = "validationExecutor")
    public Executor validationExecutor() {
        return Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }
}
