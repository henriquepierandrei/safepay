package tech.safepay.Enums;

public enum RuleType {

    // =========================
    // VALOR & LIMITE
    // =========================

    AMOUNT_THRESHOLD,
    // Dispara quando o valor da transação ultrapassa um limite absoluto definido

    DYNAMIC_AMOUNT_THRESHOLD,
    // Limite de valor calculado dinamicamente com base no histórico do cartão

    CREDIT_LIMIT_RATIO,
    // Valor da transação representa uma porcentagem elevada do limite de crédito

    CUMULATIVE_AMOUNT_WINDOW,
    // Soma dos valores em uma janela de tempo excede o limite permitido


    // =========================
    // VELOCIDADE & FREQUÊNCIA
    // =========================

    TRANSACTION_VELOCITY,
    // Número de transações em curto período acima do esperado

    RAPID_SEQUENCE,
    // Transações consecutivas com intervalo extremamente curto

    FREQUENCY_SPIKE,
    // Aumento súbito na frequência de uso do cartão

    ATTEMPT_RATE_LIMIT,
    // Muitas tentativas de transação em sequência (aprovadas ou não)


    // =========================
    // LOCALIZAÇÃO & GEO
    // =========================

    LOCATION_CHANGE,
    // Mudança repentina de localização em relação ao histórico do cartão

    IMPOSSIBLE_TRAVEL,
    // Localizações incompatíveis com deslocamento físico no tempo disponível

    COUNTRY_MISMATCH,
    // País da transação difere do país habitual ou de emissão

    HIGH_RISK_LOCATION,
    // Transação originada de região classificada como alto risco


    // =========================
    // COMERCIANTE & CATEGORIA
    // =========================

    MERCHANT_RISK,
    // Comerciante classificado como alto risco ou com histórico suspeito

    UNUSUAL_MERCHANT,
    // Comerciante fora do padrão de consumo do cartão

    MERCHANT_CATEGORY_ANOMALY,
    // Categoria de comerciante incomum para o perfil do usuário

    NEW_MERCHANT_FIRST_USE,
    // Primeira interação do cartão com um comerciante específico


    // =========================
    // DISPOSITIVO & REDE
    // =========================

    DEVICE_CHANGE,
    // Uso de dispositivo nunca visto anteriormente

    DEVICE_FINGERPRINT_MISMATCH,
    // Alteração relevante no fingerprint do dispositivo

    IP_RISK,
    // IP associado a VPN, proxy, TOR ou reputação negativa

    MULTI_CARD_DEVICE,
    // Vários cartões sendo usados no mesmo dispositivo


    // =========================
    // PADRÕES DE FRAUDE CLÁSSICOS
    // =========================

    CARD_TESTING_PATTERN,
    // Sequência de pequenas transações para testar validade do cartão

    MICRO_TRANSACTION_PATTERN,
    // Muitas transações de valor muito baixo em curto período

    DECLINE_APPROVE_PATTERN,
    // Várias recusas seguidas de aprovação bem-sucedida

    REVERSAL_PATTERN,
    // Alto volume de estornos ou cancelamentos


    // =========================
    // SCORE & MODELO
    // =========================

    FRAUD_SCORE_THRESHOLD,
    // Disparo baseado em score de fraude acima do limite

    ANOMALY_SCORE_THRESHOLD,
    // Score de anomalia excede o valor aceitável

    MODEL_CONFIDENCE_RULE
    // Decisão baseada no nível de confiança do modelo
}
