package tech.safepay.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tech.safepay.Enums.AlertType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converter JPA responsável por transformar uma lista de {@link AlertType}
 * em uma representação {@link String} para persistência no banco de dados
 * e vice-versa.
 * <p>
 * O formato persistido utiliza valores {@code Enum.name()},
 * separados por vírgula.
 * </p>
 *
 * Exemplo de valor no banco:
 * {@code "SUSPICIOUS_DEVICE,VELOCITY_CHECK"}
 *
 * <p>
 * Regras de conversão:
 * <ul>
 *   <li>{@code null} ou lista vazia → {@code null} no banco</li>
 *   <li>{@code null} ou string vazia no banco → lista vazia</li>
 * </ul>
 * </p>
 *
 * @author SafePay Team
 * @version 1.0
 */
@Converter
public class AlertTypeConverter
        implements AttributeConverter<List<AlertType>, String> {

    /**
     * Delimitador utilizado para separar os valores no banco de dados.
     */
    private static final String DELIMITER = ",";

    /**
     * Converte uma lista de {@link AlertType} para uma {@link String}
     * compatível com persistência em banco de dados.
     *
     * @param alertTypes lista de tipos de alerta
     * @return string contendo os nomes dos enums separados por vírgula,
     *         ou {@code null} caso a lista seja nula ou vazia
     */
    @Override
    public String convertToDatabaseColumn(List<AlertType> alertTypes) {
        if (alertTypes == null || alertTypes.isEmpty()) {
            return null;
        }

        return alertTypes.stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    /**
     * Converte a representação persistida no banco de dados
     * para uma lista de {@link AlertType}.
     *
     * @param dbData string persistida no banco
     * @return lista de {@link AlertType}; nunca {@code null}
     */
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
