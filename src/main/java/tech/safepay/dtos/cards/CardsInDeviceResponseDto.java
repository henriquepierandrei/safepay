package tech.safepay.dtos.cards;

import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta contendo os cartões vinculados a um dispositivo específico.
 * <p>
 * Utilizado em endpoints que retornam a relação entre dispositivos
 * e seus respectivos cartões.
 * </p>
 *
 * @param cards lista de cartões associados ao dispositivo
 *
 * @author SafePay Team
 * @version 1.0
 */
public record CardsInDeviceResponseDto(
        List<CardResponseDto> cards
) {

    /**
     * DTO interno que representa os dados essenciais de um cartão
     * vinculado a um dispositivo.
     *
     * @param cardId identificador único do cartão
     * @param cardNumber número mascarado do cartão
     * @param cardHolderName nome do portador do cartão
     * @param cardBrand bandeira do cartão
     * @param expirationDate data de expiração do cartão
     * @param creditLimit limite de crédito disponível
     * @param status status atual do cartão
     */
    public record CardResponseDto(
            UUID cardId,
            String cardNumber,
            String cardHolderName,
            CardBrand cardBrand,
            LocalDate expirationDate,
            BigDecimal creditLimit,
            CardStatus status
    ) {
    }
}
