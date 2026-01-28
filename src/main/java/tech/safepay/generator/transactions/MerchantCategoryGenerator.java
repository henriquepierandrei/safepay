package tech.safepay.generator.transactions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.MerchantCategory;
import tech.safepay.entities.Card;
import tech.safepay.repositories.TransactionRepository;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * Gerador inteligente de categorias de comerciantes baseado em histórico comportamental e padrões de risco.
 * <p>
 * Este componente utiliza algoritmo de seleção ponderada para gerar categorias de comerciantes
 * que simulam comportamento realista de usuários, incluindo:
 * <ul>
 *   <li>Categorias consistentes com histórico de compras (90% dos casos)</li>
 *   <li>Categorias de alto risco para teste de fraude (10% dos casos)</li>
 *   <li>Pesos dinâmicos baseados em frequência histórica</li>
 *   <li>Distribuição probabilística realista</li>
 * </ul>
 * <p>
 * <b>Estratégia de geração:</b>
 * <ol>
 *   <li>Analisa últimas 20 transações do cartão</li>
 *   <li>Constrói mapa de pesos por categoria (frequência histórica)</li>
 *   <li>Decide entre padrão normal (90%) ou alto risco (10%)</li>
 *   <li>Aplica seleção aleatória ponderada</li>
 * </ol>
 * <p>
 * <b>Sistema de pesos:</b>
 * <ul>
 *   <li><b>Peso base:</b> 1 (todas as categorias têm chance mínima)</li>
 *   <li><b>Peso histórico:</b> +3 por ocorrência nas últimas 20 transações</li>
 *   <li><b>Resultado:</b> categorias frequentes têm probabilidade muito maior de repetição</li>
 * </ul>
 * <p>
 * <b>Categorias de alto risco:</b>
 * Sistema mantém lista de categorias suspeitas para detecção de fraude:
 * <ul>
 *   <li>GAMBLING (jogos de azar)</li>
 *   <li>CRYPTO_EXCHANGE (exchanges de criptomoeda)</li>
 *   <li>MONEY_TRANSFER (transferências monetárias)</li>
 *   <li>ADULT_CONTENT (conteúdo adulto)</li>
 * </ul>
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Geração de transações de teste com categorias realistas</li>
 *   <li>Simulação de mudanças comportamentais súbitas</li>
 *   <li>Testes de regras antifraude baseadas em categoria</li>
 *   <li>Criação de datasets para treinamento de modelos ML</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class MerchantCategoryGenerator {

    /**
     * Gerador de números aleatórios thread-safe para seleção de categorias.
     */
    private static final Random RANDOM = new Random();

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Constrói mapa de pesos para cada categoria de comerciante baseado em histórico de transações.
     * <p>
     * Este método implementa a primeira etapa do algoritmo de geração, analisando o
     * comportamento histórico do cartão e atribuindo pesos que refletem a frequência
     * de cada categoria.
     * <p>
     * <b>Algoritmo de pesos:</b>
     * <ol>
     *   <li>Inicializa todas as categorias com peso base 1</li>
     *   <li>Recupera últimas 20 transações do cartão</li>
     *   <li>Para cada transação histórica: adiciona +3 ao peso da categoria</li>
     *   <li>Resultado: categorias frequentes acumulam peso muito maior</li>
     * </ol>
     * <p>
     * <b>Exemplo de cálculo de pesos:</b>
     * <table border="1">
     *   <tr>
     *     <th>Categoria</th>
     *     <th>Ocorrências nas últimas 20</th>
     *     <th>Cálculo</th>
     *     <th>Peso final</th>
     *     <th>Probabilidade aproximada</th>
     *   </tr>
     *   <tr>
     *     <td>GROCERY</td>
     *     <td>8</td>
     *     <td>1 + (8 × 3)</td>
     *     <td>25</td>
     *     <td>~42%</td>
     *   </tr>
     *   <tr>
     *     <td>RESTAURANT</td>
     *     <td>5</td>
     *     <td>1 + (5 × 3)</td>
     *     <td>16</td>
     *     <td>~27%</td>
     *   </tr>
     *   <tr>
     *     <td>GAS_STATION</td>
     *     <td>3</td>
     *     <td>1 + (3 × 3)</td>
     *     <td>10</td>
     *     <td>~17%</td>
     *   </tr>
     *   <tr>
     *     <td>ENTERTAINMENT</td>
     *     <td>0</td>
     *     <td>1 + (0 × 3)</td>
     *     <td>1</td>
     *     <td>~2%</td>
     *   </tr>
     *   <tr>
     *     <td>Outras (10 categorias)</td>
     *     <td>0 cada</td>
     *     <td>1 cada</td>
     *     <td>1 cada (10 total)</td>
     *     <td>~17% total</td>
     *   </tr>
     *   <tr>
     *     <td colspan="3"><b>Soma total</b></td>
     *     <td><b>60</b></td>
     *     <td><b>100%</b></td>
     *   </tr>
     * </table>
     * <p>
     * <b>Por que peso base = 1?</b>
     * Garante que mesmo categorias nunca utilizadas tenham chance mínima de serem
     * selecionadas, simulando comportamento realista onde usuários ocasionalmente
     * compram em categorias novas.
     * <p>
     * <b>Por que incremento = 3?</b>
     * O multiplicador 3 foi calibrado empiricamente para criar diferença significativa
     * entre categorias frequentes e raras, sem eliminar completamente a aleatoriedade.
     * Valor menor (1-2) resulta em distribuição muito uniforme; valor maior (5+) resulta
     * em comportamento excessivamente determinístico.
     * <p>
     * <b>Limitação a 20 transações:</b>
     * Janela deslizante de 20 transações equilibra:
     * <ul>
     *   <li>Memória suficiente para identificar padrões</li>
     *   <li>Adaptação rápida a mudanças de comportamento</li>
     *   <li>Performance de query no banco de dados</li>
     * </ul>
     *
     * @param card cartão para análise de histórico de transações
     * @return mapa EnumMap contendo cada categoria e seu peso calculado (≥1)
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
     * Seleciona uma categoria de comerciante aleatoriamente respeitando distribuição de pesos.
     * <p>
     * Este método implementa algoritmo de seleção aleatória ponderada (weighted random selection),
     * garantindo que categorias com maior peso tenham proporcionalmente maior probabilidade
     * de serem escolhidas.
     * <p>
     * <b>Algoritmo de seleção ponderada:</b>
     * <ol>
     *   <li>Calcula soma total de todos os pesos</li>
     *   <li>Gera número aleatório entre 0 e (total - 1)</li>
     *   <li>Percorre categorias acumulando pesos</li>
     *   <li>Retorna primeira categoria onde acumulado ultrapassa número aleatório</li>
     * </ol>
     * <p>
     * <b>Exemplo visual do algoritmo:</b>
     * <pre>
     * Pesos: GROCERY=25, RESTAURANT=16, GAS=10, ENTERTAINMENT=1, OTHER=8
     * Total: 60
     *
     * Representação linear:
     * [GROCERY: 0-24][RESTAURANT: 25-40][GAS: 41-50][ENT: 51][OTHER: 52-59]
     *
     * Random gerado: 32
     *
     * Iteração 1: acumulado = 0 + 25 = 25  → 32 não é < 25, continua
     * Iteração 2: acumulado = 25 + 16 = 41 → 32 é < 41, RETORNA RESTAURANT ✓
     * </pre>
     * <p>
     * <b>Complexidade:</b>
     * <ul>
     *   <li><b>Tempo:</b> O(n) onde n = número de categorias (~13 categorias)</li>
     *   <li><b>Espaço:</b> O(1) apenas variáveis locais</li>
     * </ul>
     * <p>
     * <b>Garantias matemáticas:</b>
     * <ul>
     *   <li>Probabilidade exata: P(categoria) = peso(categoria) / soma(todos os pesos)</li>
     *   <li>Distribuição uniforme se todos os pesos forem iguais</li>
     *   <li>Sempre retorna alguma categoria (fallback para UNKNOWN se necessário)</li>
     * </ul>
     * <p>
     * <b>Caso extremo - fallback:</b>
     * Se o loop não retornar (teoricamente impossível com implementação correta),
     * retorna {@code MerchantCategory.UNKNOWN} como fallback de segurança.
     * Isso nunca deve ocorrer em operação normal.
     *
     * @param weights mapa de categorias com seus pesos (valores ≥1)
     * @return categoria selecionada aleatoriamente respeitando pesos, ou UNKNOWN se erro
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
     * Gera categoria de comerciante para uma transação, decidindo entre padrão normal e alto risco.
     * <p>
     * Este é o método principal do gerador, orquestrando todo o processo de seleção de categoria
     * com decisão probabilística entre comportamento normal e anômalo.
     * <p>
     * <b>Fluxo de decisão:</b>
     * <ol>
     *   <li>Gera número aleatório (0.0 a 1.0)</li>
     *   <li>Se < 0.10 (10%): retorna categoria de alto risco aleatória</li>
     *   <li>Se ≥ 0.10 (90%): aplica seleção ponderada baseada em histórico</li>
     * </ol>
     * <p>
     * <b>Distribuição estatística:</b>
     * <table border="1">
     *   <tr>
     *     <th>Tipo de comportamento</th>
     *     <th>Probabilidade</th>
     *     <th>Método utilizado</th>
     *     <th>Propósito</th>
     *   </tr>
     *   <tr>
     *     <td>Normal (baseado em histórico)</td>
     *     <td>90%</td>
     *     <td>buildCategoryWeights + weightedRandom</td>
     *     <td>Simular uso típico e consistente</td>
     *   </tr>
     *   <tr>
     *     <td>Alto risco (anômalo)</td>
     *     <td>10%</td>
     *     <td>randomHighRiskCategory</td>
     *     <td>Testar detecção de fraude</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Categorias de alto risco:</b>
     * Quando o 10% anômalo é acionado, seleciona aleatoriamente entre:
     * <ul>
     *   <li><b>GAMBLING:</b> jogos de azar online, cassinos, apostas</li>
     *   <li><b>CRYPTO_EXCHANGE:</b> compra/venda de criptomoedas</li>
     *   <li><b>MONEY_TRANSFER:</b> transferências monetárias, remessas</li>
     *   <li><b>ADULT_CONTENT:</b> conteúdo adulto, entretenimento adulto</li>
     * </ul>
     * <p>
     * <b>Por que 10% de alto risco?</b>
     * Taxa calibrada para:
     * <ul>
     *   <li>Exercitar regras antifraude de categoria suspeita</li>
     *   <li>Simular comportamento anômalo realista (mudança súbita de padrão)</li>
     *   <li>Não criar falsos positivos excessivos em sistemas de alerta</li>
     *   <li>Manter datasets de teste balanceados (90% normal, 10% suspeito)</li>
     * </ul>
     * <p>
     * <b>Uso em detecção de fraude:</b>
     * As categorias geradas permitem testar:
     * <ul>
     *   <li>Alertas por categoria incomum para o perfil do cartão</li>
     *   <li>Scores de risco elevados em categorias de alto risco</li>
     *   <li>Padrões de mudança comportamental súbita</li>
     *   <li>Bloqueios automáticos em categorias proibidas</li>
     * </ul>
     * <p>
     * <b>Exemplo de comportamento:</b>
     * <pre>
     * Cartão com histórico:
     * - 10 transações em GROCERY
     * - 5 transações em RESTAURANT
     * - 3 transações em GAS_STATION
     * - 2 transações em PHARMACY
     *
     * Próximas 100 transações geradas:
     * - ~40 em GROCERY (peso 31)
     * - ~23 em RESTAURANT (peso 16)
     * - ~14 em GAS_STATION (peso 10)
     * - ~9 em PHARMACY (peso 7)
     * - ~4 distribuídas em outras categorias (peso 1 cada)
     * - ~10 em categorias de alto risco (GAMBLING, CRYPTO, etc.)
     * </pre>
     *
     * @param card cartão para o qual gerar a categoria de comerciante
     * @return categoria selecionada seguindo distribuição probabilística (90% normal, 10% risco)
     */
    public MerchantCategory sortMerchant(Card card) {

        // 10% de chance de comportamento fora do padrão
        if (RANDOM.nextDouble() < 0.10) {
            return randomHighRiskCategory();
        }

        var weights = buildCategoryWeights(card);
        return weightedRandom(weights);
    }

    /**
     * Seleciona aleatoriamente uma categoria de alto risco para simular transação suspeita.
     * <p>
     * Este método retorna uma das quatro categorias classificadas como de alto risco
     * para detecção de fraude, com distribuição uniforme entre elas.
     * <p>
     * <b>Categorias de alto risco e suas características:</b>
     * <ul>
     *   <li><b>GAMBLING (Jogos de azar):</b>
     *     <ul>
     *       <li>Cassinos online, apostas esportivas, poker</li>
     *       <li>Alto risco de vício e lavagem de dinheiro</li>
     *       <li>Frequentemente alvo de fraude com cartões roubados</li>
     *     </ul>
     *   </li>
     *   <li><b>CRYPTO_EXCHANGE (Exchanges de criptomoeda):</b>
     *     <ul>
     *       <li>Compra/venda de Bitcoin, Ethereum, altcoins</li>
     *       <li>Transações irreversíveis facilitam fraude</li>
     *       <li>Volatilidade alta atrai comportamento suspeito</li>
     *     </ul>
     *   </li>
     *   <li><b>MONEY_TRANSFER (Transferências monetárias):</b>
     *     <ul>
     *       <li>Western Union, MoneyGram, remessas internacionais</li>
     *       <li>Difícil rastreamento e recuperação</li>
     *       <li>Muito utilizado em esquemas de fraude</li>
     *     </ul>
     *   </li>
     *   <li><b>ADULT_CONTENT (Conteúdo adulto):</b>
     *     <ul>
     *       <li>Sites de entretenimento adulto, webcams</li>
     *       <li>Alto índice de chargebacks (contestações)</li>
     *       <li>Usuários frequentemente negam transações legítimas</li>
     *     </ul>
     *   </li>
     * </ul>
     * <p>
     * <b>Distribuição:</b>
     * Cada categoria tem probabilidade de 25% (1/4) de ser selecionada quando
     * este método é chamado, garantindo cobertura uniforme de testes.
     * <p>
     * <b>Uso em antifraude:</b>
     * Estas categorias tipicamente:
     * <ul>
     *   <li>Adicionam 20-40 pontos ao score de fraude</li>
     *   <li>Disparam alertas automáticos se fora do perfil</li>
     *   <li>Requerem autenticação adicional (3DS, OTP)</li>
     *   <li>Podem ser bloqueadas em políticas corporativas</li>
     * </ul>
     *
     * @return uma das quatro categorias de alto risco selecionada uniformemente
     */
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