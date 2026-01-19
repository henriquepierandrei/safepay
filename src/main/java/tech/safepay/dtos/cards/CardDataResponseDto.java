package tech.safepay.dtos.cards;

import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

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
