package tech.safepay.services;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertStatus;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Factory responsável pela construção de alertas de fraude consolidados.
 * <p>
 * Esta classe atua como uma ponte entre o motor de detecção de fraude e o sistema de alertas,
 * traduzindo scores técnicos e sinais de fraude em objetos de negócio compreensíveis.
 * <p>
 * <b>Responsabilidades:</b>
 * <ul>
 *   <li>Construir objetos FraudAlert a partir de scores e sinais de fraude</li>
 *   <li>Classificar severidade baseada em thresholds de score</li>
 *   <li>Calcular probabilidade normalizada de fraude</li>
 *   <li>Gerar descrições auditáveis para equipes de risco e compliance</li>
 * </ul>
 * <p>
 * <b>O que esta classe NÃO faz:</b>
 * <ul>
 *   <li>Não decide se uma transação é fraudulenta</li>
 *   <li>Não executa validações de dados</li>
 *   <li>Não persiste dados no banco</li>
 *   <li>Não calcula scores de fraude</li>
 * </ul>
 * <p>
 * A factory atua exclusivamente na transformação de dados técnicos em artefatos
 * de negócio, simulando a resposta estruturada de um motor antifraude externo.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class FraudAlertFactory {

    /**
     * Cria um alerta de fraude consolidado a partir dos dados da transação e análise de risco.
     * <p>
     * Este método constrói um objeto FraudAlert completo contendo:
     * <ul>
     *   <li><b>Contexto:</b> transação e cartão associados</li>
     *   <li><b>Sinais:</b> tipos de alerta identificados e score de fraude</li>
     *   <li><b>Classificação:</b> severidade e probabilidade calculadas</li>
     *   <li><b>Metadados:</b> timestamp de criação e status inicial (PENDING)</li>
     *   <li><b>Descrição:</b> texto legível para análise humana</li>
     * </ul>
     * <p>
     * <b>Fluxo de processamento:</b>
     * <ol>
     *   <li>Associa o alerta à transação e cartão de origem</li>
     *   <li>Consolida os tipos de alerta detectados</li>
     *   <li>Determina severidade baseada no score de fraude</li>
     *   <li>Calcula probabilidade normalizada (0-100%)</li>
     *   <li>Gera descrição auditável do alerta</li>
     *   <li>Define status inicial como PENDING para revisão</li>
     * </ol>
     * <p>
     * <b>Nota importante:</b> Este método não valida se a transação é realmente fraudulenta.
     * Ele apenas estrutura os sinais já processados pelo motor de detecção em um formato
     * adequado para armazenamento, análise e tomada de decisão.
     *
     * @param transaction transação sob análise que originou o alerta
     * @param alertTypes lista de tipos de alerta identificados (ex: HIGH_VALUE, VELOCITY_CHECK)
     * @param fraudScore score numérico de risco calculado pelo motor antifraude (0-100+)
     * @return objeto FraudAlert completo e pronto para persistência
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
     * Traduz o score numérico de fraude em uma classificação de severidade operacional.
     * <p>
     * A severidade determina a prioridade e a ação requerida pelas equipes de análise:
     * <ul>
     *   <li><b>CRITICAL (≥100):</b> Bloqueio imediato recomendado / ação automática</li>
     *   <li><b>HIGH (70-99):</b> Revisão manual prioritária obrigatória</li>
     *   <li><b>MEDIUM (50-69):</b> Revisão manual recomendada em até 24h</li>
     *   <li><b>LOW (&lt;50):</b> Monitoramento passivo / análise posterior</li>
     * </ul>
     * <p>
     * <b>Estratégia de classificação:</b>
     * <ul>
     *   <li>Scores críticos (≥100) indicam múltiplos sinais fortes de fraude confirmada</li>
     *   <li>Scores altos (70-99) requerem intervenção humana imediata</li>
     *   <li>Scores médios (50-69) justificam investigação não urgente</li>
     *   <li>Scores baixos (&lt;50) são tratados com monitoramento de rotina</li>
     * </ul>
     * <p>
     * Estes thresholds foram calibrados com base em análise histórica de fraudes
     * e devem ser periodicamente revisados conforme evolução dos padrões.
     *
     * @param score score de fraude calculado (tipicamente 0-100, mas pode exceder)
     * @return severidade operacional correspondente ao score
     */
    private Severity resolveSeverity(int score) {
        if (score >= 100) return Severity.CRITICAL;
        if (score >= 70) return Severity.HIGH;
        if (score >= 50) return Severity.MEDIUM;
        return Severity.LOW;
    }

    /**
     * Calcula a probabilidade normalizada de fraude em formato percentual.
     * <p>
     * Este método simula a saída de um modelo de machine learning ou motor antifraude
     * externo que retorna probabilidades calibradas entre 0% e 100%.
     * <p>
     * <b>Implementação atual:</b>
     * <ul>
     *   <li>Trata o score de entrada como já sendo um percentual</li>
     *   <li>Limita o valor máximo em 100% para consistência</li>
     *   <li>Não aplica transformações não-lineares ou calibrações</li>
     * </ul>
     * <p>
     * <b>Observação para produção:</b>
     * Em cenários reais, esta probabilidade viria de:
     * <ul>
     *   <li>Modelos de machine learning treinados (Random Forest, XGBoost, Neural Networks)</li>
     *   <li>Calibração isotônica ou Platt scaling</li>
     *   <li>Ensemble de múltiplos modelos</li>
     *   <li>APIs de provedores de antifraude (Sift, Kount, Forter)</li>
     * </ul>
     *
     * @param score score de fraude bruto
     * @return probabilidade de fraude normalizada (0-100%)
     */
    private int calculateProbability(int score) {
        return Math.min(score, 100);
    }

    /**
     * Gera uma descrição textual resumida e auditável do alerta de fraude.
     * <p>
     * As descrições são estruturadas para serem:
     * <ul>
     *   <li><b>Compreensíveis:</b> linguagem clara para analistas não-técnicos</li>
     *   <li><b>Acionáveis:</b> indicam claramente o nível de ação necessário</li>
     *   <li><b>Auditáveis:</b> adequadas para logs de compliance e relatórios regulatórios</li>
     *   <li><b>Consistentes:</b> padronizadas por faixa de risco</li>
     * </ul>
     * <p>
     * <b>Categorização de mensagens:</b>
     * <table border="1">
     *   <tr>
     *     <th>Score</th>
     *     <th>Descrição</th>
     *     <th>Público-alvo</th>
     *   </tr>
     *   <tr>
     *     <td>≥80</td>
     *     <td>Risco crítico com múltiplos sinais fortes</td>
     *     <td>Equipe de prevenção / Segurança</td>
     *   </tr>
     *   <tr>
     *     <td>50-79</td>
     *     <td>Alto risco requerendo revisão prioritária</td>
     *     <td>Analistas de fraude</td>
     *   </tr>
     *   <tr>
     *     <td>30-49</td>
     *     <td>Comportamento atípico para monitoramento</td>
     *     <td>Equipe de monitoramento</td>
     *   </tr>
     *   <tr>
     *     <td>&lt;30</td>
     *     <td>Risco baixo dentro do esperado</td>
     *     <td>Logs automáticos</td>
     *   </tr>
     * </table>
     * <p>
     * As mensagens são intencionalmente genéricas para proteger detalhes técnicos
     * do motor antifraude contra possível exposição em interfaces de usuário.
     *
     * @param score score de fraude que determina a mensagem apropriada
     * @return descrição textual do alerta adequada para consumo por equipes de risco e compliance
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