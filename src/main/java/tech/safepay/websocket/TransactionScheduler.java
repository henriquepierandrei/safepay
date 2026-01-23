package tech.safepay.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.services.TransactionPipelineService;

import java.time.Instant;

@Service
public class TransactionScheduler {


    private static final Logger log = LoggerFactory.getLogger(TransactionScheduler.class);
    private final TransactionExecutionControl executionControl;
    private final TransactionPipelineService pipelineService;
    private final TransactionWebSocketPublisher publisher;

    public TransactionScheduler(TransactionExecutionControl executionControl, TransactionPipelineService pipelineService, TransactionWebSocketPublisher publisher) {
        this.executionControl = executionControl;
        this.pipelineService = pipelineService;
        this.publisher = publisher;
    }

    @Scheduled(fixedRate = 60000)
    public void run() {

        if (executionControl.isPaused()) {
            return; // Não processa a transação!
        }

        log.info(Instant.now() +  " | Transação sendo processada...");
        TransactionResponseDto result =
                pipelineService.process(false, false, null);

        publisher.publish(result);
    }

}
