package tech.safepay.dtos.cards;

import org.springframework.http.HttpStatus;

public record CardRegisterResponse(
        HttpStatus status,
        String message,
        Boolean isCreated
) {
}
