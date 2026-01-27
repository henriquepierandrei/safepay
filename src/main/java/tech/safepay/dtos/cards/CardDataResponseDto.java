package tech.safepay.dtos.cards;

import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de resposta contendo os dados principais de um cartão.
 * <p>
 * Utilizado em endpoints de consulta e listagem,
 * representando o estado atual do cartão no sistema SafePay.
 * </p>
 *
 * @param cardId identificador único do cartão
 * @param cardNumber número mascarado do cartão
 * @param cardHolderName nome do portador do cartão
 * @param cardBrand bandeira do cartão
 * @param expirationDate data de expiração do cartão
 * @param creditLimit limite de crédito disponível
 * @param status status atual do cartão
 *
 * @author SafePay Team
 * @version 1.0
 */
public record CardDataResponseDto(
        UUID cardId,
        String cardNumber,
        String cardHolderName,
        CardBrand cardBrand,
        LocalDate expirationDate,
        BigDecimal creditLimit,
        CardStatus status
) {
}
