package tech.safepay.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tech.safepay.repositories.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ResetDataService {


    private static final Logger log = LoggerFactory.getLogger(ResetDataService.class);
    private final CardPatternRepository cardPatternRepository;
    private final TransactionRepository transactionRepository;
    private final FraudAlertRepository alertRepository;
    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    private final CardService cardService;
    private final DeviceService deviceService;

    public ResetDataService(CardPatternRepository cardPatternRepository, TransactionRepository transactionRepository, FraudAlertRepository alertRepository, CardRepository cardRepository, DeviceRepository deviceRepository, CardService cardService, DeviceService deviceService) {
        this.cardPatternRepository = cardPatternRepository;
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
        this.cardService = cardService;
        this.deviceService = deviceService;
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public void resetAllData() {
        long start = System.currentTimeMillis();

        try {
            // 1️⃣ Limpeza paralela (I/O bound)
            List<Callable<Void>> deleteTasks = List.of(
                    () -> { cardPatternRepository.deleteAll(); return null; },
                    () -> { transactionRepository.deleteAll(); return null; },
                    () -> { alertRepository.deleteAll(); return null; },
                    () -> { cardRepository.deleteAll(); return null; },
                    () -> { deviceRepository.deleteAll(); return null; }
            );

            executor.invokeAll(deleteTasks);

            // 2️⃣ Criação de dados (sequencial por regra de negócio)
            cardService.cardRegister(10000);
            deviceService.generateDevice(10000);

            // 3️⃣ Associação (depende dos dois acima)
            deviceService.addCardToDeviceAutomatic();

            log.info("Reset completo executado em {} ms",
                    System.currentTimeMillis() - start);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Erro ao executar reset", e);
        }
    }
}
