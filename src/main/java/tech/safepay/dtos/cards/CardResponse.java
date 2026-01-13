package tech.safepay.dtos.cards;

import org.springframework.http.HttpStatus;

public record CardResponse(
        HttpStatus status,
        String message
) {
}
