package tech.safepay.dtos.cards;

import org.springframework.http.HttpStatus;

/**
 * DTO de resposta padrão para operações relacionadas a cartões.
 * <p>
 * Utilizado para retornar o status da operação e uma mensagem
 * descritiva ao cliente após ações como criação, exclusão
 * ou atualização de cartões.
 * </p>
 *
 * @param status status HTTP representando o resultado da operação
 * @param message mensagem descritiva do resultado
 *
 * @author SafePay Team
 * @version 1.0
 */
public record CardResponse(
        HttpStatus status,
        String message
) {
}
