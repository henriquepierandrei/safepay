package tech.safepay.generator.transactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.DeviceType;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Component
public class TransactionGenerator {
    // Iniciando variável estática
    private static final Random RANDOM = new Random();

    private final MerchantCategoryGenerator merchantCategoryGenerator;
    private final AmountGenerator amountGenerator;
    private final IPGenerator ipGenerator;
    private final LatitudeAndLongitudeGenerator generateLocation;


    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;


    public TransactionGenerator(MerchantCategoryGenerator merchantCategoryGenerator, AmountGenerator amountGenerator, IPGenerator ipGenerator, LatitudeAndLongitudeGenerator generateLocation, CardRepository cardRepository,
                                TransactionRepository transactionRepository, DeviceRepository deviceRepository) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.ipGenerator = ipGenerator;
        this.generateLocation = generateLocation;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.deviceRepository = deviceRepository;
    }

    // Escolher cartão a ser utilizado.
    private Card sortCard(){
        var cards = cardRepository.findAll();
        var position = RANDOM.nextInt(cards.size());
        return cards.get(position);
    }



    public Transaction generateNormalTransaction(){
        Transaction transaction = new Transaction();
        var card = sortCard();


        transaction.setCard(card);

        // Selecione o tipo de compra (Mercado, Cosméticos etc)
        transaction.setMerchantCategory(merchantCategoryGenerator.sortMerchant(card));

        // No momento de gerar o valor da transação, também verificará se:
        // 1 - Cartão já foi expirado;
        // 2- Limite de crédito foi alcançado;
        // 3 - Status do cartão (bloqueado, ativo ou perdido).
        var amount = amountGenerator.generateAmount(card);
        transaction.setAmount(amount);

        // Definindo horário
        transaction.setTransactionDateAndTime(LocalDateTime.now());


        // Definindo IP
        transaction.setIpAddress(ipGenerator.generateIP());



        // Definindo dispositivo
        var devices = card.getDevices();

        if (devices == null || devices.isEmpty()) {
            throw new IllegalStateException(
                    "Card " + card.getCardId() + " has no associated devices"
            );
        }

        transaction.setDevice(
                devices.get(RANDOM.nextInt(devices.size()))
        );

        // Gera a Latitude e Longitude basiado nas transações anteriores.
        String[] location = generateLocation.generateLocation(card);
        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);

        transactionRepository.saveAndFlush(transaction);

        // Descontando o valor do limite de credito
        card.setCreditLimit(card.getCreditLimit().subtract(amount));
        cardRepository.saveAndFlush(card);


        return null;

    }





}
