package tech.safepay.dtos.transaction;

import tech.safepay.Enums.MerchantCategory;
import tech.safepay.Enums.Severity;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.dtos.validation.ValidationResultDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de resposta consolidada do processamento de uma transação.
 * <p>
 * Representa o resultado final do pipeline antifraude, incluindo:
 * dados da transação, contexto de dispositivo e localização,
 * validações executadas, severidade, decisão final e status de fraude.
 * </p>
 *
 * Utilizado como payload de resposta nos endpoints de processamento
 * automático e manual de transações.
 *
 * @author SafePay Team
 * @version 1.0
 */
public record TransactionResponseDto(

        /** Dados do cartão utilizado na transação */
        CardDataResponseDto card,

        /** Categoria do estabelecimento (MCC simplificado) */
        MerchantCategory merchantCategory,

        /** Valor monetário da transação */
        BigDecimal amount,

        /** Data e hora em que a transação ocorreu */
        LocalDateTime transactionDateAndTime,

        /** Latitude informada ou detectada */
        String latitude,

        /** Longitude informada ou detectada */
        String longitude,

        /** Localização resolvida a partir de coordenadas */
        ResolvedLocalizationDto localizationDto,

        /** Resultado das validações antifraude executadas */
        ValidationResultDto validations,

        /** Severidade global calculada pelo pipeline */
        Severity severity,

        /** Dispositivo associado à transação */
        DeviceListResponseDto.DeviceDto device,

        /** Endereço IP de origem da transação */
        String ipAddress,

        /** Decisão final do motor antifraude */
        TransactionDecision transactionDecision,

        /** Flag indicando se a transação foi classificada como fraude */
        Boolean isFraud,

        /** Timestamp de criação do registro */
        LocalDateTime createdAt
) {
}
