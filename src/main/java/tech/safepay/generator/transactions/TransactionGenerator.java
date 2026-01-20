package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class TransactionGenerator {

    private static final Random RANDOM = new Random();

    private final MerchantCategoryGenerator merchantCategoryGenerator;
    private final AmountGenerator amountGenerator;
    private final IPGenerator ipGenerator;
    private final LatitudeAndLongitudeGenerator generateLocation;

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;

    public TransactionGenerator(
            MerchantCategoryGenerator merchantCategoryGenerator,
            AmountGenerator amountGenerator,
            IPGenerator ipGenerator,
            LatitudeAndLongitudeGenerator generateLocation,
            CardRepository cardRepository,
            TransactionRepository transactionRepository,
            DeviceRepository deviceRepository
    ) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.ipGenerator = ipGenerator;
        this.generateLocation = generateLocation;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Seleciona um cartão aleatório disponível
     */
    private Card sortCard() {
        var cards = cardRepository.findAll();

        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards available to generate transactions");
        }

        return cards.get(RANDOM.nextInt(cards.size()));
    }

    /**
     * Gera uma transação simulando comportamento legítimo.
     *
     * Importante:
     * - Não executa decisão antifraude
     * - Não calcula score
     * - Apenas cria o evento transacional
     *
     * A decisão ocorre posteriormente no pipeline antifraude.
     */
    public Transaction generateNormalTransaction() {

        Card card = sortCard();
        Transaction transaction = new Transaction();

        transaction.setCard(card);

        // Categoria do merchant baseada no perfil do cartão
        transaction.setMerchantCategory(
                merchantCategoryGenerator.sortMerchant(card)
        );

        // Valor da transação (respeita limite, status e expiração)
        transaction.setAmount(
                amountGenerator.generateAmount(card)
        );

        transaction.setTransactionDateAndTime(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());

        // IP
        transaction.setIpAddress(
                ipGenerator.generateIP()
        );

        // Device
        var devices = card.getDevices();
        if (devices == null || devices.isEmpty()) {
            throw new IllegalStateException(
                    "Card " + card.getCardId() + " has no associated devices"
            );
        }

        transaction.setDevice(
                devices.get(RANDOM.nextInt(devices.size()))
        );

        // Localização baseada no histórico do cartão
        String[] location = generateLocation.generateLocation(card);
        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);

        // Persistência inicial (estado neutro)
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setFraud(false);

        return transactionRepository.saveAndFlush(transaction);
    }
}
