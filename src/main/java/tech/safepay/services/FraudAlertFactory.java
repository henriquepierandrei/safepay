package tech.safepay.services;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FraudAlertFactory {

    /**
     * =========================
     * FRAUD ALERT FACTORY
     * =========================
     *
     * Responsabilidade:
     * Construir um objeto FraudAlert consolidado a partir
     * do score final de risco calculado pelo motor antifraude.
     *
     * Importante:
     * - Esta classe NÃO decide fraude
     * - NÃO executa validações
     * - NÃO persiste dados
     *
     * Ela apenas traduz sinais técnicos em um artefato
     * de negócio compreensível (alerta antifraude),
     * simulando a resposta de um motor antifraude externo.
     */
    public FraudAlert create(
            Transaction transaction,
            List<AlertType> alertTypes,
            int fraudScore
    ) {

        FraudAlert alert = new FraudAlert();

        // =========================
        // CONTEXTO
        // =========================
        alert.setTransaction(transaction);
        alert.setCard(transaction.getCard());

        // =========================
        // SINAIS CONSOLIDADOS
        // =========================
        alert.setAlertTypes(alertTypes);
        alert.setFraudScore(fraudScore);

        // =========================
        // METADADOS
        // =========================
        alert.setCreatedAt(LocalDateTime.now());
        alert.setStatus(AlertStatus.PENDING);

        // =========================
        // CLASSIFICAÇÃO DE RISCO
        // =========================
        alert.setSeverity(resolveSeverity(fraudScore));
        alert.setFraudProbability(calculateProbability(fraudScore));

        // =========================
        // DESCRIÇÃO HUMANA
        // =========================
        alert.setDescription(buildDescription(fraudScore));

        return alert;
    }

    /**
     * Traduz o score numérico em severidade operacional.
     *
     * Estratégia:
     * - LOW: apenas monitoramento
     * - MEDIUM: revisão recomendada
     * - HIGH: revisão prioritária
     * - CRITICAL: bloqueio imediato / ação automática
     */
    private Severity resolveSeverity(int score) {
        if (score >= 100) return Severity.CRITICAL;
        if (score >= 70) return Severity.HIGH;
        if (score >= 50) return Severity.MEDIUM;
        return Severity.LOW;
    }

    /**
     * Simula probabilidade de fraude retornada por
     * um motor antifraude externo (ex: score normalizado).
     *
     * Observação:
     * Aqui o score já é tratado como percentual,
     * mas em cenários reais isso viria de modelos ML.
     */
    private int calculateProbability(int score) {
        return Math.min(score, 100);
    }

    /**
     * Gera uma descrição resumida e auditável
     * para times de risco, suporte e compliance.
     */
    private String buildDescription(int score) {

        if (score >= 80) {
            return "Risco crítico detectado. Múltiplos sinais fortes de fraude.";
        }

        if (score >= 50) {
            return "Alto risco de fraude. Revisão manual prioritária recomendada.";
        }

        if (score >= 30) {
            return "Comportamento fora do padrão identificado. Monitoramento necessário.";
        }

        return "Risco baixo. Transação dentro do comportamento esperado.";
    }
}
