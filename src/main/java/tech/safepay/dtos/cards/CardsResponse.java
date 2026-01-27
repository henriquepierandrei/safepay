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

/**
 * DTO de resposta contendo informações completas de um cartão.
 * <p>
 * Utilizado em consultas detalhadas e listagens administrativas,
 * expondo dados de vínculo com dispositivos, limites,
 * status de risco e metadados temporais.
 * </p>
 *
 * @param cardId identificador único do cartão
 * @param cardNumber número mascarado do cartão
 * @param cardHolderName nome do portador do cartão
 * @param cardBrand bandeira do cartão
 * @param deviceIds lista de identificadores dos dispositivos vinculados
 * @param expirationDate data de expiração do cartão
 * @param creditLimit limite total de crédito
 * @param remainingLimit limite de crédito restante
 * @param status status atual do cartão
 * @param riskScore score de risco associado ao cartão
 * @param createdAt data e hora de criação do cartão
 * @param lastTransactionAt data e hora da última transação realizada
 *
 * @author SafePay Team
 * @version 1.0
 */
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

    /**
     * Converte uma entidade {@link Card} em um {@link CardsResponse}.
     * <p>
     * Realiza o mapeamento dos dispositivos associados,
     * expondo apenas seus identificadores técnicos.
     * </p>
     *
     * @param card entidade de cartão
     * @return DTO {@link CardsResponse} correspondente à entidade informada
     */
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
