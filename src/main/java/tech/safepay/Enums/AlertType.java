package tech.safepay.Enums;

public enum AlertType {

    // =========================
    // VALOR & LIMITE
    // =========================

    HIGH_AMOUNT(20),
    // Valor significativamente acima da média histórica do cartão.
    // Forte quando combinado com device/local novo.

    LIMIT_EXCEEDED(40),
    // Tentativa de transação acima do limite disponível.
    // Não é fraude por si só, mas é risco operacional alto.


    // =========================
    // FREQUÊNCIA & VELOCIDADE
    // =========================

    VELOCITY_ABUSE(35),
    // Muitas transações em curto intervalo.
    // Um dos sinais mais fortes de fraude real.

    BURST_ACTIVITY(25),
    // Pico súbito de atividade fora do padrão histórico.
    // Complementa VELOCITY olhando o “formato” do comportamento.


    // =========================
    // LOCALIZAÇÃO & GEO
    // =========================

    LOCATION_ANOMALY(20),
    // Local fora do padrão histórico do cartão.
    // Sinal moderado, depende de contexto.

    IMPOSSIBLE_TRAVEL(45),
    // Distância incompatível com o tempo entre transações.
    // Quase determinístico de fraude.

    HIGH_RISK_COUNTRY(40),
    // Origem em país classificado como alto risco.
    // Forte, mas não decisivo sozinho.

    // =========================
    // DISPOSITIVO & REDE                   falta fazer tbm
    // =========================

    NEW_DEVICE_DETECTED(15),
    // Device nunca utilizado anteriormente.
    // Sozinho é fraco, mas ótimo em combinação.

    DEVICE_FINGERPRINT_CHANGE(25),
    // Alteração relevante no fingerprint do dispositivo.
    // Indica troca real de ambiente ou tentativa de evasão.

    TOR_OR_PROXY_DETECTED(35),
    // Uso de VPN, proxy ou TOR.
    // Forte indicativo quando somado a outros sinais.

    MULTIPLE_CARDS_SAME_DEVICE(50),
    // Vários cartões usados no mesmo dispositivo.
    // Indicador clássico de fraude em escala.


    // =========================
    // COMPORTAMENTO DO USUÁRIO
    // =========================

    TIME_OF_DAY_ANOMALY(10),
    // Horário atípico para o usuário.
    // Sinal fraco, mas barato e útil como complemento.




    // =========================
    // PADRÕES DE FRAUDE CLÁSSICOS
    // =========================

    CARD_TESTING(50),
    // Pequenas transações repetidas para validar cartão.
    // Altíssimo valor preditivo.

    MICRO_TRANSACTION_PATTERN(35),
    // Sequência de valores muito baixos.
    // Variante de card testing, mas menos agressiva.

    DECLINE_THEN_APPROVE_PATTERN(30),
    // Múltiplas recusas seguidas de aprovação.
    // Muito comum após brute force de dados.



    // =========================
    // RISCO OPERACIONAL
    // =========================

    MULTIPLE_FAILED_ATTEMPTS(25),
    // Diversas tentativas falhas consecutivas.
    // Forte quando combinado com velocity.

    SUSPICIOUS_SUCCESS_AFTER_FAILURE(35),
    // Aprovação após sequência de falhas.
    // Clássico padrão de ataque bem-sucedido.


    // =========================
    // MODELO & SCORE
    // =========================

    ANOMALY_MODEL_TRIGGERED(30),
    // Modelo estatístico detectou padrão fora da normalidade.
    // Deve pesar, mas nunca decidir sozinho.

    GENERIC_RISK(0);


    private final int score;

    AlertType(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
