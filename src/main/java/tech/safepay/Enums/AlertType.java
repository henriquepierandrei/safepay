package tech.safepay.Enums;

public enum AlertType {

    // =========================
    // VALOR & LIMITE
    // =========================

    HIGH_AMOUNT,
    // Transação com valor significativamente acima do padrão histórico do cartão

    LIMIT_EXCEEDED,
    // Tentativa de transação acima do limite de crédito disponível

    SUDDEN_AMOUNT_SPIKE,
    // Aumento abrupto no valor médio das transações em curto período

    ROUND_AMOUNT_ANOMALY,
    // Valor “redondo” atípico (ex: 1000, 5000), comum em testes de fraude


    // =========================
    // FREQUÊNCIA & VELOCIDADE
    // =========================

    VELOCITY_ABUSE,
    // Muitas transações em um curto intervalo de tempo

    RAPID_FIRE_TRANSACTIONS,
    // Sequência extremamente rápida de transações consecutivas

    BURST_ACTIVITY,
    // Pico súbito de atividade fora do comportamento normal

    TRANSACTION_FREQUENCY_ANOMALY,
    // Frequência de transações acima do padrão esperado


    // =========================
    // LOCALIZAÇÃO & GEO
    // =========================

    LOCATION_ANOMALY,
    // Transação em localização geográfica incomum para o cartão

    IMPOSSIBLE_TRAVEL,
    // Transações em locais distantes em intervalo incompatível com deslocamento físico

    COUNTRY_MISMATCH,
    // País diferente do histórico ou do país de emissão do cartão

    HIGH_RISK_COUNTRY,
    // Transação originada de país classificado como alto risco


    // =========================
    // DISPOSITIVO & REDE
    // =========================

    NEW_DEVICE_DETECTED,
    // Transação realizada a partir de um dispositivo nunca visto antes

    DEVICE_FINGERPRINT_CHANGE,
    // Alteração significativa no fingerprint do dispositivo

    IP_ANOMALY,
    // Endereço IP incomum ou fora do padrão do usuário

    TOR_OR_PROXY_DETECTED,
    // Uso de VPN, proxy ou rede TOR para ocultar origem

    MULTIPLE_CARDS_SAME_DEVICE,
    // Vários cartões usados no mesmo dispositivo (indicador de fraude em massa)


    // =========================
    // COMPORTAMENTO DO USUÁRIO
    // =========================

    UNUSUAL_MERCHANT,
    // Estabelecimento fora do perfil de consumo do cartão

    UNUSUAL_MERCHANT_CATEGORY,
    // Categoria de comerciante incomum para o histórico do usuário

    TIME_OF_DAY_ANOMALY,
    // Transação realizada em horário atípico (ex: madrugada)

    WEEKEND_ANOMALY,
    // Atividade em finais de semana fora do padrão histórico


    // =========================
    // PADRÕES DE FRAUDE CLÁSSICOS
    // =========================

    CARD_TESTING,
    // Pequenas transações repetidas para testar validade do cartão

    MICRO_TRANSACTION_PATTERN,
    // Várias transações de valor muito baixo em sequência

    REVERSAL_ABUSE,
    // Muitos estornos ou cancelamentos em curto período

    DECLINE_THEN_APPROVE_PATTERN,
    // Várias tentativas recusadas seguidas de aprovação


    // =========================
    // RISCO OPERACIONAL
    // =========================

    MULTIPLE_FAILED_ATTEMPTS,
    // Diversas tentativas falhas consecutivas

    SUSPICIOUS_SUCCESS_AFTER_FAILURE,
    // Transação aprovada após múltiplas falhas suspeitas

    MANUAL_REVIEW_REQUIRED,
    // Caso marcado explicitamente para revisão humana

    RULE_ENGINE_CONFLICT,
    // Conflito entre regras de fraude (ex: score alto vs regra permissiva)


    // =========================
    // SCORE & MODELO
    // =========================

    HIGH_FRAUD_SCORE,
    // Score de fraude acima do limite definido

    MODEL_CONFIDENCE_LOW,
    // Modelo de detecção com baixa confiança na decisão

    ANOMALY_MODEL_TRIGGERED
    // Modelo de detecção de anomalias identificou padrão fora da normalidade
}
