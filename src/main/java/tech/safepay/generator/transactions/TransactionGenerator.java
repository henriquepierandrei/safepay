package tech.safepay.generator.transactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;

import java.util.Random;

@Component
public class TransactionGenerator {
    // Iniciando variável estática
    private static final Random RANDOM = new Random();

    private final MerchantCategoryGenerator merchantCategoryGenerator;
    private final AmountGenerator amountGenerator;
    private final CardRepository cardRepository;


    public TransactionGenerator(MerchantCategoryGenerator merchantCategoryGenerator, AmountGenerator amountGenerator, CardRepository cardRepository) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.cardRepository = cardRepository;
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
        transaction.setMerchantCategory(merchantCategoryGenerator.sortMerchant(card));

        // No momento de gerar o valor da transação, também verificará se:
        // 1 - Cartão já foi expirado;
        // 2- Limite de crédito foi alcançado;
        // 3 - Status do cartão (bloqueado, ativo ou perdido).

    }


}
