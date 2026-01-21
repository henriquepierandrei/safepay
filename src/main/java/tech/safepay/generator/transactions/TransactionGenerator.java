package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.TransactionStatus;
import tech.safepay.configs.ResolveLocalizationConfig;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Component
public class TransactionGenerator {

    private static final Random RANDOM = new Random();

    private final MerchantCategoryGenerator merchantCategoryGenerator;
    private final AmountGenerator amountGenerator;
    private final IPGenerator ipGenerator;
    private final LatitudeAndLongitudeGenerator generateLocation;

    private final ResolveLocalizationConfig resolveLocalizationConfig;

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
            DeviceRepository deviceRepository,
            ResolveLocalizationConfig resolveLocalizationConfig
    ) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.ipGenerator = ipGenerator;
        this.generateLocation = generateLocation;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.deviceRepository = deviceRepository;
        this.resolveLocalizationConfig = resolveLocalizationConfig;
    }

    /**
     * Seleciona um cartão aleatório disponível
     */
    private Card sortCard() {
        var cards = cardRepository.findByDevicesIsNotEmpty();

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

        var device = devices.get(RANDOM.nextInt(devices.size()));

        transaction.setDevice(device);
        transaction.setDeviceFingerprint(device.getFingerPrintId());

        // Localização baseada no histórico do cartão
        String[] location = generateLocation.generateLocation(card);
        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);


        // Localizacao exata

        ResolvedLocalizationDto resolvedLocalizationDto = resolveLocalizationConfig.resolve(location[0], location[1]);
        transaction.setCountryCode(resolvedLocalizationDto.countryCode());
        transaction.setState(resolvedLocalizationDto.state());
        transaction.setCity(resolvedLocalizationDto.city());


        // Persistência inicial (estado neutro)
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setFraud(false);

        return transactionRepository.save(transaction);
    }




    /**
     * Gera uma transação de forma manual e determinística.
     *
     * Este método é utilizado para criação controlada de transações em cenários como:
     * - Testes funcionais e de integração
     * - Simulação de cenários específicos de fraude
     * - Reprodução de incidentes
     * - Operações administrativas internas
     *
     * Importante:
     * - Não executa validações antifraude
     * - Não calcula score ou probabilidade de fraude
     * - Não altera limites ou status do cartão
     *
     * A transação é persistida em estado neutro (TransactionStatus.PENDING)
     * e será posteriormente processada pelo pipeline antifraude.
     *
     * @param manualTransactionDto DTO contendo todos os dados necessários
     *                             para criação explícita da transação
     * @return transação persistida no banco de dados
     * @throws IllegalArgumentException se o cartão ou o device não existirem
     * @throws IllegalStateException se o device não estiver vinculado ao cartão
     */
    public Transaction generateManualTransaction(ManualTransactionDto manualTransactionDto) {

        // Recupera o cartão informado
        Card card = cardRepository.findById(manualTransactionDto.cardId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Card not found")
                );

        // Recupera o device informado
        var device = deviceRepository.findById(manualTransactionDto.deviceId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Device not found")
                );

        // Garante que o device pertence ao cartão informado
        if (card.getDevices() == null || !card.getDevices().contains(device)) {
            throw new IllegalStateException(
                    "Device does not belong to the card"
            );
        }

        Transaction transaction = new Transaction();
        transaction.setCard(card);
        transaction.setDevice(device);
        transaction.setDeviceFingerprint(device.getFingerPrintId());

        transaction.setAmount(manualTransactionDto.amount());
        transaction.setMerchantCategory(manualTransactionDto.merchantCategory());

        transaction.setIpAddress(manualTransactionDto.ipAddress());
        transaction.setLatitude(manualTransactionDto.latitude());
        transaction.setLongitude(manualTransactionDto.longitude());

        ResolvedLocalizationDto resolvedLocalizationDto = resolveLocalizationConfig.resolve(manualTransactionDto.latitude(), manualTransactionDto.longitude());
        transaction.setCountryCode(resolvedLocalizationDto.countryCode());
        transaction.setState(resolvedLocalizationDto.state());
        transaction.setCity(resolvedLocalizationDto.city());


        transaction.setTransactionDateAndTime(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());

        // Estado inicial da transação
        transaction.setTransactionStatus(TransactionStatus.PENDING);
        transaction.setFraud(false);

        return transactionRepository.save(transaction);
    }


}
