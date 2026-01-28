package tech.safepay.validations;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validador de padrões comportamentais suspeitos em transações.
 * <p>
 * Esta classe identifica sequências e comportamentos característicos de fraude,
 * incluindo técnicas comuns utilizadas por fraudadores para testar cartões roubados,
 * sondar sistemas antifraude e executar ataques de força bruta.
 * </p>
 * <p>
 * Os padrões detectados incluem:
 * <ul>
 *   <li><strong>Card Testing:</strong> múltiplas transações de baixo valor para validar cartões</li>
 *   <li><strong>Micro Transactions:</strong> sequências de valores irrisórios para sondar limites</li>
 *   <li><strong>Decline-Then-Approve:</strong> tentativas repetidas até conseguir aprovação</li>
 * </ul>
 * </p>
 * <p>
 * Estes validadores focam em <strong>análise temporal e volumétrica</strong>,
 * complementando outras camadas de segurança como análise de dispositivo e geolocalização.
 * </p>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class FraudPatternsValidation {

    /**
     * Detecta tentativas de validação de cartão através de múltiplas transações de baixo valor.
     * <p>
     * <strong>Card Testing</strong> é uma técnica comum onde fraudadores executam diversas
     * transações de valores muito baixos em curto intervalo para verificar se um cartão
     * roubado está ativo e funcional, antes de realizar compras de alto valor.
     * </p>
     *
     * <h3>Estratégia de Detecção</h3>
     * <ul>
     *   <li><strong>Janela temporal:</strong> últimos 10 minutos</li>
     *   <li><strong>Foco:</strong> volume e valor das transações</li>
     *   <li><strong>Threshold:</strong> múltiplas transações abaixo de R$ 2,00 ou R$ 5,00</li>
     * </ul>
     *
     * <h3>Regras de Ativação</h3>
     * <pre>
     * ALERTA SE:
     *   - 3+ transações com valor ≤ R$ 2,00 (valores irrisórios)
     *   OU
     *   - 5+ transações com valor ≤ R$ 5,00 (valores baixos)
     * </pre>
     *
     * <h3>Cenário Típico de Fraude</h3>
     * <pre>
     * 10:00:05 - R$ 1,00 ✓
     * 10:00:32 - R$ 0,50 ✓
     * 10:01:15 - R$ 1,50 ✓
     * 10:02:08 - R$ 2,00 ✓
     * → CARD_TESTING detectado (4 transações ≤ R$ 2,00)
     * </pre>
     *
     * <h3>Observações Importantes</h3>
     * <ul>
     *   <li>Altíssimo valor preditivo quando detectado</li>
     *   <li>Frequentemente combinado com sinais de dispositivo suspeito</li>
     *   <li>Pode preceder ataques de alto valor</li>
     *   <li>Score: <strong>50 pontos</strong> (peso alto)</li>
     * </ul>
     *
     * @param transaction a transação atual sendo analisada (não pode ser null)
     * @param snapshot snapshot contendo histórico recente de transações do cartão
     * @return {@link ValidationResultDto} contendo:
     *         <ul>
     *           <li>Score de 50 pontos se padrão for detectado</li>
     *           <li>Alerta {@link AlertType#CARD_TESTING}</li>
     *           <li>Objeto vazio se padrão não for encontrado</li>
     *         </ul>
     *
     * @see AlertType#CARD_TESTING
     * @see ValidationResultDto
     * @see TransactionGlobalValidation.ValidationSnapshot#last10Minutes()
     */
    public ValidationResultDto cardTestingPattern(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Obtém transações dos últimos 10 minutos
        List<Transaction> recentTransactions = snapshot.last10Minutes();

        // Conta transações com valores irrisórios (≤ R$ 2,00)
        long veryLowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        // Conta transações com valores baixos (≤ R$ 5,00)
        long lowValueCount = recentTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(5)) <= 0)
                .count();

        // Aplica regra de detecção
        if (veryLowValueCount >= 3 || lowValueCount >= 5) {
            result.addScore(AlertType.CARD_TESTING.getScore());
            result.addAlert(AlertType.CARD_TESTING);
        }

        return result;
    }

    /**
     * Identifica sequências de microtransações utilizadas para sondar o sistema antifraude.
     * <p>
     * <strong>Micro Transaction Pattern</strong> detecta quando um cartão executa
     * múltiplas transações de valores extremamente baixos, geralmente como forma de
     * testar limites do sistema antes de realizar um ataque de maior escala.
     * </p>
     *
     * <h3>Diferença para Card Testing</h3>
     * <ul>
     *   <li>Volume menor de transações</li>
     *   <li>Valores ainda mais irrisórios</li>
     *   <li>Pode ocorrer em janela temporal maior</li>
     *   <li>Foco em proporção, não velocidade</li>
     * </ul>
     *
     * <h3>Estratégia de Detecção</h3>
     * <ul>
     *   <li><strong>Janela:</strong> últimas 20 transações do cartão</li>
     *   <li><strong>Análise:</strong> proporcional (ratio-based)</li>
     *   <li><strong>Threshold:</strong> 60% ou mais com valor ≤ R$ 2,00</li>
     * </ul>
     *
     * <h3>Regra de Ativação</h3>
     * <pre>
     * ALERTA SE:
     *   (transações ≤ R$ 2,00) / (total de transações) ≥ 0.6
     *   E histórico mínimo de 5 transações
     * </pre>
     *
     * <h3>Exemplo de Detecção</h3>
     * <pre>
     * Histórico (10 transações):
     *   R$ 1,00, R$ 0,50, R$ 1,50, R$ 2,00, R$ 1,00,
     *   R$ 0,75, R$ 1,25, R$ 50,00, R$ 1,00, R$ 1,50
     *
     * Microtransações: 9 de 10 = 90%
     * → MICRO_TRANSACTION_PATTERN detectado
     * </pre>
     *
     * <h3>Contexto de Uso</h3>
     * <ul>
     *   <li>Fraudadores testando regras antifraude</li>
     *   <li>Reconhecimento de limites do sistema</li>
     *   <li>Preparação para ataque subsequente</li>
     *   <li>Score: <strong>35 pontos</strong> (peso médio-alto)</li>
     * </ul>
     *
     * @param transaction a transação atual sendo analisada (não pode ser null)
     * @param snapshot snapshot contendo histórico recente de transações do cartão
     * @return {@link ValidationResultDto} contendo:
     *         <ul>
     *           <li>Score de 35 pontos se padrão for detectado</li>
     *           <li>Alerta {@link AlertType#MICRO_TRANSACTION_PATTERN}</li>
     *           <li>Objeto vazio se padrão não for encontrado ou histórico insuficiente</li>
     *         </ul>
     *
     * @see AlertType#MICRO_TRANSACTION_PATTERN
     * @see ValidationResultDto
     * @see TransactionGlobalValidation.ValidationSnapshot#last20()
     */
    public ValidationResultDto microTransactionPattern(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();
        if (card == null) return result;

        // Obtém últimas 20 transações
        List<Transaction> lastTransactions = snapshot.last20();

        // Histórico insuficiente para análise
        if (lastTransactions.size() < 5) return result;

        // Conta microtransações (≤ R$ 2,00)
        long microCount = lastTransactions.stream()
                .filter(t -> t.getAmount().compareTo(BigDecimal.valueOf(2)) <= 0)
                .count();

        // Calcula proporção de microtransações
        double ratio = (double) microCount / lastTransactions.size();

        // Aplica regra de detecção (60% ou mais)
        if (ratio >= 0.6) {
            result.addScore(AlertType.MICRO_TRANSACTION_PATTERN.getScore());
            result.addAlert(AlertType.MICRO_TRANSACTION_PATTERN);
        }

        return result;
    }

    /**
     * Detecta padrão de múltiplas recusas seguidas por aprovação bem-sucedida.
     * <p>
     * <strong>Decline-Then-Approve Pattern</strong> identifica sequências características
     * de ataques de força bruta, onde fraudadores tentam repetidamente até conseguir
     * uma aprovação, geralmente testando diferentes combinações de CVV, data de validade
     * ou ajustando valores até passar pelos filtros.
     * </p>
     *
     * <h3>Contexto Típico de Fraude</h3>
     * <ul>
     *   <li><strong>Brute force de CVV:</strong> tenta 000, 001, 002... até acertar</li>
     *   <li><strong>Teste de validade:</strong> experimenta diferentes datas</li>
     *   <li><strong>Ajuste de limite:</strong> reduz valor até ser aprovado</li>
     * </ul>
     *
     * <h3>Estratégia de Detecção</h3>
     * <ul>
     *   <li><strong>Janela:</strong> últimas 10 transações do cartão</li>
     *   <li><strong>Requisito:</strong> transação atual deve estar APROVADA</li>
     *   <li><strong>Busca:</strong> recusas consecutivas imediatamente antes</li>
     * </ul>
     *
     * <h3>Regra de Ativação</h3>
     * <pre>
     * ALERTA SE:
     *   - Transação atual = APPROVED
     *   E
     *   - 3+ das últimas transações = BLOCKED
     *   E
     *   - Histórico mínimo de 4 transações
     * </pre>
     *
     * <h3>Sequência Típica Detectada</h3>
     * <pre>
     * Transação #1: BLOCKED (CVV incorreto)
     * Transação #2: BLOCKED (CVV incorreto)
     * Transação #3: BLOCKED (CVV incorreto)
     * Transação #4: APPROVED (CVV correto - fraudador acertou!)
     * → DECLINE_THEN_APPROVE_PATTERN detectado
     * </pre>
     *
     * <h3>Observações Importantes</h3>
     * <ul>
     *   <li>Muito comum em ataques automatizados (bots)</li>
     *   <li>Forte indicador quando combinado com device/IP suspeito</li>
     *   <li>Sugere persistência maliciosa do atacante</li>
     *   <li>Score: <strong>30 pontos</strong> (peso médio)</li>
     * </ul>
     *
     * @param transaction a transação atual sendo analisada (não pode ser null)
     * @param snapshot snapshot contendo histórico recente de transações do cartão
     * @return {@link ValidationResultDto} contendo:
     *         <ul>
     *           <li>Score de 30 pontos se padrão for detectado</li>
     *           <li>Alerta {@link AlertType#DECLINE_THEN_APPROVE_PATTERN}</li>
     *           <li>Objeto vazio se padrão não for encontrado, transação não aprovada, ou histórico insuficiente</li>
     *         </ul>
     *
     * @see AlertType#DECLINE_THEN_APPROVE_PATTERN
     * @see TransactionDecision#APPROVED
     * @see TransactionDecision#BLOCKED
     * @see ValidationResultDto
     * @see TransactionGlobalValidation.ValidationSnapshot#last10()
     */
    public ValidationResultDto declineThenApprovePattern(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        Card card = transaction.getCard();

        // Só analisa se cartão existe e transação foi aprovada
        if (card == null || transaction.getTransactionDecision() != TransactionDecision.APPROVED) {
            return result;
        }

        // Obtém últimas 10 transações
        List<Transaction> lastTransactions = snapshot.last10();

        // Histórico insuficiente para análise
        if (lastTransactions.size() < 4) return result;

        // Conta transações bloqueadas nas 3 tentativas anteriores
        long declinedCount = lastTransactions.stream()
                .skip(1) // Ignora a transação atual (que está aprovada)
                .limit(3) // Analisa as 3 anteriores
                .filter(t -> t.getTransactionDecision() == TransactionDecision.BLOCKED)
                .count();

        // Aplica regra de detecção (3 bloqueios seguidos)
        if (declinedCount >= 3) {
            result.addScore(AlertType.DECLINE_THEN_APPROVE_PATTERN.getScore());
            result.addAlert(AlertType.DECLINE_THEN_APPROVE_PATTERN);
        }

        return result;
    }
}