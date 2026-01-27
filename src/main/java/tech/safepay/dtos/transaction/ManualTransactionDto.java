package tech.safepay.dtos.transaction;

import tech.safepay.Enums.MerchantCategory;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO utilizado para criação manual de transações.
 * <p>
 * Representa uma solicitação explícita de transação enviada via API,
 * normalmente usada para testes, simulações ou operações administrativas.
 * </p>
 *
 * <p>
 * Este DTO não executa validações de negócio — essas responsabilidades
 * pertencem à camada de serviço.
 * </p>
 *
 * @param cardId identificador do cartão utilizado na transação
 * @param deviceId identificador do dispositivo onde a transação foi originada
 * @param amount valor monetário da transação
 * @param merchantCategory categoria do estabelecimento comercial
 * @param ipAddress endereço IP de origem da transação
 * @param latitude latitude geográfica do local da transação
 * @param longitude longitude geográfica do local da transação
 *
 * @author SafePay Team
 * @version 1.0
 */
public record ManualTransactionDto(
        UUID cardId,
        UUID deviceId,
        BigDecimal amount,
        MerchantCategory merchantCategory,
        String ipAddress,
        String latitude,
        String longitude
) {
}
