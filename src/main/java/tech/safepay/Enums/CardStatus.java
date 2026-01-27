package tech.safepay.Enums;

/**
 * Define o estado atual de um cartão.
 */
public enum CardStatus {

    /** Cartão ativo e apto para transações. */
    ACTIVE,

    /** Cartão bloqueado por segurança ou solicitação do cliente. */
    BLOCKED,

    /** Cartão reportado como perdido ou roubado. */
    LOST
}
