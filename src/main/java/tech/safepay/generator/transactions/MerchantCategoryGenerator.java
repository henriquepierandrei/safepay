package tech.safepay.generator.transactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.repositories.TransactionRepository;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

@Component
public class MerchantCategoryGenerator {
    private static final Random RANDOM = new Random();


    @Autowired
    private TransactionRepository transactionRepository;



    /** 1 - Aqui verifica as últimas transações e categoriza-as com seus respectivos pesos.
     * Quanto mais comum for a transação maior será peso dela, ou seja, caso haja transações em outras categorias, ela não será frequente logo ela obterá menor peso.
     * @param card - Recebe o cartão para buscar as últimas 20 transações
     * @return - Retorna cada categoria com seu respectivo peso.
     */
    private Map<MerchantCategory, Integer> buildCategoryWeights(Card card) {
        Map<MerchantCategory, Integer> weights = new EnumMap<>(MerchantCategory.class);

        // inicializa peso base
        for (MerchantCategory category : MerchantCategory.values()) {
            weights.put(category, 1);
        }

        var lastTransactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        for (var tx : lastTransactions) {
            weights.merge(tx.getMerchantCategory(), 3, Integer::sum);
        }

        return weights;
    }


    /**
     * 2 - Escolhe uma MerchantCategory aleatoriamente, mas respeitando pesos.
     *
     * Ou seja:
     * quanto maior o peso, maior a chance de ser escolhida.
     * @param weights - Map com as categorias e seus respectivos pesos
     * @return - Returna uma categoria já filtrada e sorteada baseado nos pesos!
     */
    private MerchantCategory weightedRandom(Map<MerchantCategory, Integer> weights) {

        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int random = RANDOM.nextInt(total);

        int cumulative = 0;
        for (var entry : weights.entrySet()) {
            cumulative += entry.getValue();
            if (random < cumulative) {
                return entry.getKey();
            }
        }

        return MerchantCategory.UNKNOWN;
    }


    /**
     * 3 - Última etapa, aqui reune ambos os metodos e sorteia uma categoria de compra
     * @param card - Cartão para buscar as transacões
     * @return - Retorna uma categoria
     */
    public MerchantCategory sortMerchant(Card card) {

        // 10% de chance de comportamento fora do padrão
        if (RANDOM.nextDouble() < 0.10) {
            return randomHighRiskCategory();
        }

        var weights = buildCategoryWeights(card);
        return weightedRandom(weights);
    }

    private MerchantCategory randomHighRiskCategory() {
        MerchantCategory[] risky = {
                MerchantCategory.GAMBLING,
                MerchantCategory.CRYPTO_EXCHANGE,
                MerchantCategory.MONEY_TRANSFER,
                MerchantCategory.ADULT_CONTENT
        };

        return risky[RANDOM.nextInt(risky.length)];
    }
}
