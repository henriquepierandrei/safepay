package tech.safepay.Enums;

/**
 * Representa o status atual de um alerta de fraude dentro do fluxo de análise.
 *
 * <p>Esse status indica em que estágio o alerta se encontra após ser gerado
 * pelos mecanismos de validação e scoring.</p>
 *
 * <ul>
 *   <li>{@link #PENDING} – Alerta gerado, aguardando análise ou decisão.</li>
 *   <li>{@link #CONFIRMED} – Fraude confirmada após revisão automática ou manual.</li>
 *   <li>{@link #FALSE_POSITIVE} – Alerta validado como não fraudulento.</li>
 * </ul>
 *
 * <p>Usado principalmente em fluxos de revisão, auditoria e aprendizado do modelo.</p>
 */
public enum AlertStatus {

    /**
     * Alerta recém-criado e ainda não analisado.
     */
    PENDING,

    /**
     * Alerta analisado e confirmado como fraude real.
     */
    CONFIRMED,

    /**
     * Alerta analisado e classificado como falso positivo.
     */
    FALSE_POSITIVE
}
