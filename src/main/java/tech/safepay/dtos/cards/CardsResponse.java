package tech.safepay.dtos.cards;

import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CardsResponse(
        UUID cardId,
        String cardNumber,
        String cardHolderName,
        CardBrand cardBrand,
        List<UUID> deviceIds,
        LocalDate expirationDate,
        BigDecimal creditLimit,
        BigDecimal remainingLimit,
        CardStatus status,
        Integer riskScore,
        LocalDateTime createdAt,
        LocalDateTime lastTransactionAt
) {
    public static CardsResponse fromEntity(Card card) {
        return new CardsResponse(
                card.getCardId(),
                card.getCardNumber(),
                card.getCardHolderName(),
                card.getCardBrand(),
                card.getDevices()
                        .stream()
                        .map(Device::getId)
                        .toList(),
                card.getExpirationDate(),
                card.getCreditLimit(),
                card.getRemainingLimit(),
                card.getStatus(),
                card.getRiskScore(),
                card.getCreatedAt(),
                card.getLastTransactionAt()
        );
    }
}
