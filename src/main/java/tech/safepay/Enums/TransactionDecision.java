package tech.safepay.Enums;

/**
 * Define a decisão final aplicada a uma transação
 * após o processamento antifraude.
 */
public enum TransactionDecision {

    /** Transação aprovada automaticamente. */
    APPROVED,

    /** Transação encaminhada para revisão manual. */
    REVIEW,

    /** Transação bloqueada por risco elevado. */
    BLOCKED
}
