package tech.safepay.configs;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tech.safepay.Enums.AlertType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class AlertTypeConverter implements AttributeConverter<List<AlertType>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<AlertType> alertTypes) {
        if (alertTypes == null || alertTypes.isEmpty()) {
            return null;
        }
        return alertTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public List<AlertType> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(dbData.split(DELIMITER))
                .map(String::trim)
                .map(AlertType::valueOf)
                .collect(Collectors.toList());
    }
}