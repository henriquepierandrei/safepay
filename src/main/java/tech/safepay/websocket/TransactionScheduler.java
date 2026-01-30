package tech.safepay.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.services.TransactionPipelineService;

/**
 * Serviço responsável pelo agendamento e execução periódica
 * do processamento de transações simuladas ou automatizadas.
 *
 * <p>Este scheduler atua como um orquestrador entre:</p>
 * <ul>
 *   <li>O controle de execução ({@link TransactionExecutionControl})</li>
 *   <li>O pipeline de processamento de transações</li>
 *   <li>A publicação dos eventos via WebSocket</li>
 * </ul>
 *
 * <p>O processamento é condicionado ao estado operacional do sistema,
 * permitindo pausas administrativas sem necessidade de desligamento
 * da aplicação.</p>
 *
 * <p>Este componente é especialmente útil para:</p>
 * <ul>
 *   <li>Testes de carga e stress</li>
 *   <li>Simulação contínua de eventos financeiros</li>
 *   <li>Demonstrações e ambientes controlados</li>
 * </ul>
 */
@Service
public class TransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransactionScheduler.class);

    /**
     * Controle centralizado do estado de execução do scheduler.
     */
    private final TransactionExecutionControl executionControl;

    /**
     * Pipeline responsável pelo processamento completo da transação,
     * incluindo validações, regras de negócio e geração de resposta.
     */
    private final TransactionPipelineService pipelineService;

    /**
     * Componente responsável pela publicação das transações
     * processadas para os clientes conectados via WebSocket.
     */
    private final TransactionWebSocketPublisher publisher;

    /**
     * Construtor com injeção de dependências.
     *
     * @param executionControl controle de execução do scheduler
     * @param pipelineService pipeline de processamento de transações
     * @param publisher publicador WebSocket de eventos de transação
     */
    public TransactionScheduler(TransactionExecutionControl executionControl,
                                TransactionPipelineService pipelineService,
                                TransactionWebSocketPublisher publisher) {
        this.executionControl = executionControl;
        this.pipelineService = pipelineService;
        this.publisher = publisher;
    }

    /**
     * Executa periodicamente o processamento de uma transação.
     *
     * <p>Configuração atual:</p>
     * <ul>
     *   <li>Execução a cada 60 segundos</li>
     *   <li>Execução condicionada ao estado operacional do sistema</li>
     * </ul>
     *
     * <p>O processamento é executado dentro de um contexto de
     * {@code RequestScope} simulado, garantindo compatibilidade
     * com componentes que dependem de escopo HTTP.</p>
     *
     * <p>Em caso de pausa administrativa, a execução é interrompida
     * de forma silenciosa e segura.</p>
     */
    @Scheduled(fixedRate = 60000)
    public void run() {
        if (executionControl.isPaused()) {
            return;
        }

        RequestScopeExecutor.executeInRequestScope(() -> {
            log.info("Processamento automático de transação iniciado.");
            TransactionResponseDto result = pipelineService.process(false, false, null);
            publisher.publish(result);
        });
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // quantidade de execuções paralelas
        scheduler.setThreadNamePrefix("transaction-scheduler-");
        return scheduler;
    }

}
