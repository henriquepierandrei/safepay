package tech.safepay.Enums;

/**
 * Representa os tipos de alertas antifraude que podem ser acionados
 * durante a análise de uma transação.
 *
 * <p>Cada alerta possui um score associado, que contribui para o
 * risco total da transação e para a decisão final.</p>
 */
public enum AlertType {

    /** Valor significativamente acima do histórico do cartão. */
    HIGH_AMOUNT(20),

    /** Tentativa de transação acima do limite disponível. */
    LIMIT_EXCEEDED(40),

    /** Muitas transações em curto intervalo de tempo. */
    VELOCITY_ABUSE(42),

    /** Pico súbito de atividade fora do padrão histórico. */
    BURST_ACTIVITY(30),

    /** Localização fora do padrão histórico do cartão. */
    LOCATION_ANOMALY(20),

    /** Distância incompatível com o tempo entre transações. */
    IMPOSSIBLE_TRAVEL(45),

    /** Transação originada em país classificado como alto risco. */
    HIGH_RISK_COUNTRY(40),

    /** Detecção de um dispositivo nunca utilizado anteriormente. */
    NEW_DEVICE_DETECTED(10),

    /** Alteração relevante no fingerprint do dispositivo. */
    DEVICE_FINGERPRINT_CHANGE(25),

    /** Uso de VPN, proxy ou rede TOR. */
    TOR_OR_PROXY_DETECTED(30),

    /** Múltiplos cartões utilizados no mesmo dispositivo. */
    MULTIPLE_CARDS_SAME_DEVICE(50),

    /** Transação realizada em horário atípico para o usuário. */
    TIME_OF_DAY_ANOMALY(6),

    /** Pequenas transações repetidas para validação do cartão. */
    CARD_TESTING(50),

    /** Sequência de microtransações suspeitas. */
    MICRO_TRANSACTION_PATTERN(35),

    /** Múltiplas recusas seguidas de aprovação. */
    DECLINE_THEN_APPROVE_PATTERN(38),

    /** Diversas tentativas falhas consecutivas. */
    MULTIPLE_FAILED_ATTEMPTS(25),

    /** Aprovação após sequência de falhas suspeitas. */
    SUSPICIOUS_SUCCESS_AFTER_FAILURE(35),

    /** Modelo estatístico detectou comportamento anômalo. */
    ANOMALY_MODEL_TRIGGERED(30),

    /** Limite de crédito atingido ou ultrapassado. */
    CREDIT_LIMIT_REACHED(40),

    /** Transação próxima da data de expiração do cartão. */
    EXPIRATION_DATE_APPROACHING(25);

    private final int score;

    AlertType(int score) {
        this.score = score;
    }

    /**
     * Retorna o score de risco associado ao alerta.
     */
    public int getScore() {
        return score;
    }

    /**
     * Converte o score do alerta em severidade.
     */
    public Severity getSeverity() {
        if (score >= 100) return Severity.CRITICAL;
        if (score >= 70) return Severity.HIGH;
        if (score >= 50) return Severity.MEDIUM;
        return Severity.LOW;
    }
}
