package tech.safepay.generator.transactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.DeviceType;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
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
    private final SortDeviceTypeGenerator sortDeviceTypeGenerator;
    private final LatitudeAndLongitudeGenerator generateLocation;


    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;


    public TransactionGenerator(MerchantCategoryGenerator merchantCategoryGenerator, AmountGenerator amountGenerator, IPGenerator ipGenerator, SortDeviceTypeGenerator sortDeviceTypeGenerator, LatitudeAndLongitudeGenerator generateLocation, CardRepository cardRepository, TransactionRepository transactionRepository) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.ipGenerator = ipGenerator;
        this.sortDeviceTypeGenerator = sortDeviceTypeGenerator;
        this.generateLocation = generateLocation;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
    }

    // Escolher cartão a ser utilizado.
    private Card sortCard(){
        var cards = cardRepository.findAll();
        var position = RANDOM.nextInt(cards.size());
        return cards.get(position);
    }



    public Transaction generateTransaction(){
        Transaction transaction = new Transaction();
        var card = sortCard();


        transaction.setCard(card);

        // Selecione o tipo de compra (Mercado, Cosméticos etc)
        transaction.setMerchantCategory(merchantCategoryGenerator.sortMerchant(card));

        // No momento de gerar o valor da transação, também verificará se:
        // 1 - Cartão já foi expirado;
        // 2- Limite de crédito foi alcançado;
        // 3 - Status do cartão (bloqueado, ativo ou perdido).
        transaction.setAmount(amountGenerator.generateAmount(card));
        transaction.setTransactionDateAndTime(LocalDateTime.now());

        transaction.setIpAdress(ipGenerator.generateIPv6());
        transaction.setDeviceType(sortDeviceTypeGenerator.sortDeviceType(card));

        // Localização geográfica da transação
        String[] location = locationGenerator.generateLocation();

        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);


        // Gera a Latitude e Longitude basiado nas transações anteriores.
        String[] location = generateLocation.generateLocation(card);
        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);




    }


}
