package tech.safepay.dtos.cards;

import java.util.UUID;

public record CardBlockResponseDto(
        UUID cardId,
        UUID deviceId,
        boolean blocked,
        String message
) {}

