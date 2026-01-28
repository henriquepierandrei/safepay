package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;

/**
 * Gerador inteligente de valores para transações simuladas baseado em histórico comportamental.
 * <p>
 * Este componente é responsável por gerar valores de transação que simulam comportamento
 * realista de usuários, incluindo:
 * <ul>
 *   <li>Valores consistentes com padrão histórico do cartão (90% dos casos)</li>
 *   <li>Valores atípicos para testar detecção de fraude (10% dos casos)</li>
 *   <li>Respeito a limites de crédito disponíveis</li>
 *   <li>Variações naturais em torno da média comportamental</li>
 * </ul>
 * <p>
 * <b>Estratégia de geração:</b>
 * <ol>
 *   <li>Analisa as últimas 20 transações do cartão</li>
 *   <li>Calcula valor médio histórico</li>
 *   <li>Decide entre valor normal (90%) ou atípico (10%)</li>
 *   <li>Aplica variações aleatórias apropriadas</li>
 *   <li>Respeita limites de crédito (com exceções controladas)</li>
 * </ol>
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Geração de transações para ambiente de testes</li>
 *   <li>Simulação de comportamento de usuários reais</li>
 *   <li>Criação de datasets para treinamento de modelos ML</li>
 *   <li>Testes de detecção de anomalias e fraudes</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class AmountGenerator {

    /**
     * Gerador de números aleatórios thread-safe para variações de valores.
     */
    private static final Random RANDOM = new Random();

    private final TransactionRepository transactionRepository;

    /**
     * Construtor do gerador com injeção de dependências.
     *
     * @param transactionRepository repositório para consulta do histórico de transações
     */
    public AmountGenerator(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Calcula o valor médio das transações recentes para estabelecer baseline comportamental.
     * <p>
     * Este método analisa o histórico de transações fornecido e calcula a média aritmética
     * dos valores. Se não houver histórico (cartão novo), retorna um valor base padrão
     * de R$ 100,00 para bootstrap inicial.
     * <p>
     * <b>Estratégia de cálculo:</b>
     * <ul>
     *   <li>Soma todos os valores das transações</li>
     *   <li>Divide pelo número de transações</li>
     *   <li>Arredonda para 2 casas decimais (HALF_UP)</li>
     * </ul>
     * <p>
     * <b>Valor padrão:</b>
     * Se o histórico estiver vazio (cartão novo ou sem transações), retorna R$ 100,00
     * como baseline inicial. Este valor foi escolhido por representar um ticket médio
     * realista para transações de varejo no Brasil.
     * <p>
     * <b>Precisão:</b>
     * Utiliza BigDecimal para garantir precisão monetária sem erros de arredondamento
     * típicos de operações com double/float.
     *
     * @param transactions lista de transações históricas para análise (tipicamente últimas 20)
     * @return valor médio das transações ou R$ 100,00 se histórico vazio
     */
    private BigDecimal calculateAverageAmount(List<Transaction> transactions) {
        if (transactions.isEmpty()) return BigDecimal.valueOf(100); // valor base inicial

        BigDecimal total = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(transactions.size()), RoundingMode.HALF_UP);
    }

    /**
     * Gera um valor de transação dentro do padrão comportamental esperado.
     * <p>
     * Este método produz valores que simulam comportamento normal de gasto,
     * aplicando uma variação aleatória de ±10% em torno da média histórica.
     * Representa os 90% de transações típicas e esperadas.
     * <p>
     * <b>Cálculo de variação:</b>
     * <ul>
     *   <li>Gera fator aleatório entre 0.9 e 1.1 (90% a 110% da média)</li>
     *   <li>Multiplica a média histórica pelo fator</li>
     *   <li>Arredonda para 2 casas decimais</li>
     * </ul>
     * <p>
     * <b>Exemplo:</b>
     * Se média histórica = R$ 200,00:
     * <ul>
     *   <li>Valor mínimo gerado: R$ 180,00 (90%)</li>
     *   <li>Valor máximo gerado: R$ 220,00 (110%)</li>
     *   <li>Distribuição uniforme no intervalo</li>
     * </ul>
     * <p>
     * <b>Realismo:</b>
     * A janela de ±10% foi calibrada para simular variações naturais de compras
     * do mesmo usuário em estabelecimentos similares, refletindo comportamento
     * consistente mas não idêntico.
     *
     * @param average valor médio histórico do cartão
     * @return valor dentro do padrão esperado (média ± 10%)
     */
    private BigDecimal generateNormalAmount(BigDecimal average) {
        double variation = 0.9 + (RANDOM.nextDouble() * 0.2); // 0.9–1.1
        return average.multiply(BigDecimal.valueOf(variation))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gera um valor de transação atípico para simular possível fraude ou gasto excepcional.
     * <p>
     * Este método produz valores significativamente acima do padrão (3-5x a média),
     * simulando os 10% de transações que podem indicar:
     * <ul>
     *   <li>Possível fraude (compra muito acima do perfil)</li>
     *   <li>Gasto excepcional legítimo (eletrodoméstico, viagem)</li>
     *   <li>Tentativa de teste de limite do cartão</li>
     * </ul>
     * <p>
     * <b>Estratégia de geração:</b>
     * <ol>
     *   <li>Multiplica média por fator entre 3x e 5x</li>
     *   <li>Em 90% dos casos: limita ao saldo disponível (transação possível)</li>
     *   <li>Em 10% dos casos: pode ultrapassar limite (testa rejeição)</li>
     * </ol>
     * <p>
     * <b>Respeitando limites (90% dos casos):</b>
     * <pre>
     * Média = R$ 100, Fator = 4x
     * Valor alto = R$ 400
     * Limite disponível = R$ 300
     * → Retorna R$ 300 (min entre valor e limite)
     * </pre>
     * <p>
     * <b>Ultrapassando limites (10% dos casos):</b>
     * <pre>
     * Média = R$ 100, Fator = 4x
     * Valor alto = R$ 400
     * Limite disponível = R$ 300
     * → Retorna R$ 400 (ignora limite - testa bloqueio)
     * </pre>
     * <p>
     * <b>Propósito dos 10% que ultrapassam:</b>
     * Essencial para testar validações de limite de crédito e garantir que
     * o sistema antifraude detecte e bloqueie corretamente tentativas de
     * transações acima do limite disponível.
     *
     * @param average valor médio histórico do cartão
     * @param card cartão para verificação de limite disponível
     * @return valor atípico (3-5x a média), potencialmente acima do limite em 10% dos casos
     */
    private BigDecimal generateHighAmount(BigDecimal average, Card card) {
        // multiplicador aleatório 3–5x a média
        BigDecimal highAmount = average.multiply(BigDecimal.valueOf(3 + RANDOM.nextInt(3)))
                .setScale(2, RoundingMode.HALF_UP);

        // 90% chance de respeitar remainingLimit, 10% chance de ultrapassar
        if (RANDOM.nextDouble() < 0.9) {
            return highAmount.min(card.getRemainingLimit());
        }

        return highAmount; // 10% chance de ultrapassar
    }

    /**
     * Gera o valor final para uma transação baseado em histórico comportamental e aleatoriedade controlada.
     * <p>
     * Este é o método principal do gerador, orquestrando toda a lógica de decisão:
     * <ol>
     *   <li>Recupera últimas 20 transações do cartão</li>
     *   <li>Calcula média histórica</li>
     *   <li>Decide entre valor normal (90%) ou atípico (10%)</li>
     *   <li>Aplica limitações de crédito quando apropriado</li>
     * </ol>
     * <p>
     * <b>Distribuição estatística:</b>
     * <table border="1">
     *   <tr>
     *     <th>Tipo</th>
     *     <th>Probabilidade</th>
     *     <th>Comportamento</th>
     *     <th>Propósito</th>
     *   </tr>
     *   <tr>
     *     <td>Normal</td>
     *     <td>90%</td>
     *     <td>Média ± 10%, respeitando limite</td>
     *     <td>Simular uso típico</td>
     *   </tr>
     *   <tr>
     *     <td>Alto (dentro do limite)</td>
     *     <td>9%</td>
     *     <td>3-5x média, limitado ao saldo</td>
     *     <td>Gastos excepcionais legítimos</td>
     *   </tr>
     *   <tr>
     *     <td>Alto (acima do limite)</td>
     *     <td>1%</td>
     *     <td>3-5x média, ignora saldo</td>
     *     <td>Testar detecção de fraude</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Tratamento de limites:</b>
     * <ul>
     *   <li>Valores normais sempre respeitam remainingLimit</li>
     *   <li>Se remainingLimit for null, usa creditLimit como fallback</li>
     *   <li>Se ambos null, assume R$ 1.000,00 como limite padrão</li>
     * </ul>
     * <p>
     * <b>Exemplo de fluxo:</b>
     * <pre>
     * Cartão com:
     * - Limite total: R$ 5.000
     * - Limite disponível: R$ 3.500
     * - Média histórica: R$ 150
     *
     * Cenário 1 (90%): Valor normal
     * → Gera entre R$ 135-165
     * → Retorna min(valor_gerado, R$ 3.500)
     * → Resultado típico: R$ 145
     *
     * Cenário 2 (9%): Valor alto dentro do limite
     * → Gera entre R$ 450-750 (3-5x média)
     * → Retorna min(valor_gerado, R$ 3.500)
     * → Resultado típico: R$ 600
     *
     * Cenário 3 (1%): Valor alto acima do limite
     * → Gera entre R$ 450-750
     * → Ignora limite disponível
     * → Resultado típico: R$ 650 (pode ser recusado pelo sistema)
     * </pre>
     * <p>
     * <b>Integração com antifraude:</b>
     * Os valores gerados são projetados para exercitar diferentes regras antifraude:
     * <ul>
     *   <li>Valores normais: testam fluxo de aprovação padrão</li>
     *   <li>Valores altos: disparam alertas de HIGH_VALUE</li>
     *   <li>Valores acima do limite: disparam CREDIT_LIMIT_REACHED</li>
     * </ul>
     *
     * @param card cartão para o qual gerar o valor da transação
     * @return valor monetário gerado (BigDecimal com 2 casas decimais)
     */
    public BigDecimal generateAmount(Card card) {
        List<Transaction> lastTx = transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);
        BigDecimal average = calculateAverageAmount(lastTx);

        if (RANDOM.nextDouble() < 0.9) {
            BigDecimal remaining = card.getRemainingLimit() != null ? card.getRemainingLimit() : card.getCreditLimit();
            if (remaining == null) remaining = BigDecimal.valueOf(1000);

            return generateNormalAmount(average).min(remaining);
        } else {
            return generateHighAmount(average, card);
        }
    }
}