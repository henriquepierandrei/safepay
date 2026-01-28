package tech.safepay.services;

import org.springframework.stereotype.Service;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.CardRepository;
import tech.safepay.validations.TransactionGlobalValidation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço responsável pelo processo de tomada de decisão sobre transações no sistema antifraude.
 * <p>
 * Este serviço orquestra o fluxo completo de avaliação de risco e decisão transacional, incluindo:
 * <ul>
 *   <li>Execução de validações antifraude e cálculo de score de risco</li>
 *   <li>Classificação automática da transação (APPROVED, REVIEW, BLOCKED)</li>
 *   <li>Geração de alertas de fraude quando aplicável</li>
 *   <li>Atualização de padrões comportamentais do cartão</li>
 *   <li>Gestão de limites de crédito e saldos disponíveis</li>
 *   <li>Registro de timestamps de última transação</li>
 * </ul>
 * <p>
 * <b>Fluxo de decisão:</b>
 * <ol>
 *   <li>Executa todas as validações antifraude (score e alertas)</li>
 *   <li>Classifica a transação baseada em thresholds de score</li>
 *   <li>Aplica overrides especiais (force approve, limite de crédito)</li>
 *   <li>Gera alertas se houver sinais de fraude</li>
 *   <li>Atualiza padrões comportamentais do cartão</li>
 *   <li>Debita limite de crédito se aprovada</li>
 * </ol>
 * <p>
 * Este serviço é o núcleo do motor de decisão antifraude e deve ser utilizado
 * como ponto único de entrada para todas as decisões transacionais.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
public class TransactionDecisionService {

    private final TransactionGlobalValidation validation;
    private final CardPatternService cardPatternService;
    private final FraudAlertFactory fraudAlertFactory;
    private final CardRepository cardRepository;

    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param validation validador global que executa todas as regras antifraude
     * @param cardPatternService serviço para construção e atualização de padrões comportamentais
     * @param fraudAlertFactory factory para criação de alertas de fraude estruturados
     * @param cardRepository repositório para persistência de alterações em cartões
     */
    public TransactionDecisionService(TransactionGlobalValidation validation, CardPatternService cardPatternService, FraudAlertFactory fraudAlertFactory,
                                      CardRepository cardRepository) {
        this.validation = validation;
        this.cardPatternService = cardPatternService;
        this.fraudAlertFactory = fraudAlertFactory;
        this.cardRepository = cardRepository;
    }

    /**
     * Resultado interno consolidado do processo de decisão antifraude.
     * <p>
     * Este record encapsula todos os artefatos gerados durante o processo de avaliação:
     * <ul>
     *   <li><b>transaction:</b> entidade da transação com decisão e flags atualizadas</li>
     *   <li><b>alerts:</b> lista de alertas de fraude gerados (vazia se nenhum alerta)</li>
     *   <li><b>fraudScore:</b> score final de risco calculado (0-100+)</li>
     * </ul>
     * <p>
     * <b>Importante:</b> Este record é para uso interno do serviço e <b>NÃO</b> deve ser
     * exposto diretamente em APIs REST. Para comunicação com clientes, utilize DTOs apropriados
     * que exponham apenas informações autorizadas.
     * <p>
     * <b>Exemplo de uso interno:</b>
     * <pre>
     * DecisionResult result = processDecision(transaction);
     * if (result.fraudScore() >= 80) {
     *     notifySecurityTeam(result.alerts());
     * }
     * </pre>
     *
     * @param transaction transação avaliada com decisão definida
     * @param alerts lista de alertas de fraude gerados durante a avaliação
     * @param fraudScore score numérico final de risco (0-100+)
     */
    public record DecisionResult(
            Transaction transaction,
            List<FraudAlert> alerts,
            int fraudScore
    ) {
    }

    /**
     * Executa o processo completo de avaliação antifraude e tomada de decisão sobre uma transação.
     * <p>
     * Este método implementa a lógica central do motor antifraude, realizando:
     * <ul>
     *   <li><b>Avaliação de risco:</b> executa todas as validações e calcula score</li>
     *   <li><b>Classificação automática:</b> define status baseado em thresholds</li>
     *   <li><b>Geração de alertas:</b> cria registros de fraude quando aplicável</li>
     *   <li><b>Atualização de padrões:</b> recalcula comportamento esperado do cartão</li>
     *   <li><b>Gestão financeira:</b> debita limite de crédito se aprovada</li>
     * </ul>
     * <p>
     * <b>Critérios de decisão por score:</b>
     * <table border="1">
     *   <tr>
     *     <th>Score</th>
     *     <th>Decisão</th>
     *     <th>Flag de Fraude</th>
     *     <th>Ação</th>
     *   </tr>
     *   <tr>
     *     <td>&lt; 25</td>
     *     <td>APPROVED</td>
     *     <td>false</td>
     *     <td>Processa normalmente, debita limite</td>
     *   </tr>
     *   <tr>
     *     <td>25-59</td>
     *     <td>REVIEW</td>
     *     <td>false</td>
     *     <td>Aguarda análise manual, não debita</td>
     *   </tr>
     *   <tr>
     *     <td>≥ 60</td>
     *     <td>BLOCKED</td>
     *     <td>true</td>
     *     <td>Rejeita imediatamente, não debita</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Regras especiais de override:</b>
     * <ul>
     *   <li><b>successForce = true:</b> força aprovação independente do score (usado em testes)</li>
     *   <li><b>CREDIT_LIMIT_REACHED:</b> força bloqueio mesmo com score baixo (limite insuficiente)</li>
     * </ul>
     * <p>
     * <b>Efeitos colaterais:</b>
     * <ul>
     *   <li>Atualiza campos da transação: decision, fraud flag</li>
     *   <li>Cria e persiste alertas de fraude se necessário</li>
     *   <li>Atualiza padrões comportamentais do cartão (cache invalidado)</li>
     *   <li>Debita limite de crédito disponível se aprovada</li>
     *   <li>Atualiza timestamp de última transação do cartão</li>
     * </ul>
     * <p>
     * <b>Performance:</b>
     * Este método executa múltiplas operações de banco de dados. Em ambientes de alta carga,
     * considere otimizações como:
     * <ul>
     *   <li>Atualização assíncrona de padrões comportamentais</li>
     *   <li>Batch processing de alertas</li>
     *   <li>Cache de padrões recentemente atualizados</li>
     * </ul>
     *
     * @param transaction transação a ser avaliada (deve estar populada com todos os dados necessários)
     * @param successForce flag de override para forçar aprovação (true = aprova independente do score)
     * @return ValidationResultDto contendo o score final e lista de alertas disparados
     */
    public ValidationResultDto evaluate(Transaction transaction, boolean successForce) {

        // 1️⃣ Executa todas as validações e coleta resultado único
        ValidationResultDto result = validation.validateAll(transaction);

        // 2️⃣ Pega score e alertas diretamente
        int totalScore = result.getScore();
        List<AlertType> triggeredAlerts = result.getTriggeredAlerts();

        // 3️⃣ Define status da transação baseado no score
        if (totalScore < 25) {
            transaction.setTransactionDecision(TransactionDecision.APPROVED);
            transaction.setFraud(false);
        } else if (totalScore >= 25 && totalScore < 60) {
            transaction.setTransactionDecision(TransactionDecision.REVIEW);
            transaction.setFraud(false);
        } else { // totalScore >= 60
            transaction.setTransactionDecision(TransactionDecision.BLOCKED);
            transaction.setFraud(true);
        }

        // 4️⃣ Override se for teste forçado
        if (successForce) {
            transaction.setTransactionDecision(TransactionDecision.APPROVED);
        }

        if (result.getTriggeredAlerts().contains(AlertType.CREDIT_LIMIT_REACHED)) {
            transaction.setTransactionDecision(TransactionDecision.BLOCKED);
        }


        // 5️⃣ Cria FraudAlert se houver alertas
        if (!triggeredAlerts.isEmpty()) {

            FraudAlert alert = fraudAlertFactory.create(
                    transaction,
                    triggeredAlerts,
                    totalScore
            );

        }


        cardPatternService.buildOrUpdateCardPattern(transaction.getCard());

        if (transaction.getTransactionDecision() == TransactionDecision.APPROVED) {
            var card = transaction.getCard();

            BigDecimal remaining = card.getRemainingLimit();
            if (remaining == null) remaining = card.getCreditLimit() != null ? card.getCreditLimit() : BigDecimal.ZERO;

            card.setRemainingLimit(remaining.subtract(transaction.getAmount()));
            card.setLastTransactionAt(LocalDateTime.now());

            cardRepository.save(card);
        }

        // 6️⃣ Retorna ValidationResultDto consolidado
        return new ValidationResultDto(totalScore, triggeredAlerts);

    }
}