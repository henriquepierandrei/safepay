package tech.safepay.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import tech.safepay.exceptions.alerts.AlertNotFoundException;
import tech.safepay.exceptions.alerts.AlertStatusNotFoundException;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.card.CardQuantityMaxException;
import tech.safepay.exceptions.device.DeviceMaxSupportedException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.exceptions.device.DeviceNotLinkedException;
import tech.safepay.exceptions.transaction.TransactionNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções da aplicação.
 *
 * <p>Centraliza o tratamento de erros, garantindo
 * respostas padronizadas, previsíveis e alinhadas
 * às boas práticas REST.</p>
 *
 * <p>Evita duplicação de código nos controllers
 * e melhora observabilidade e manutenção.</p>
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata exceções genéricas não mapeadas explicitamente.
     *
     * <p>Utilizado como fallback para erros inesperados.</p>
     *
     * @param ex exceção capturada
     * @return resposta HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Trata erros de validação ou argumentos inválidos.
     *
     * @param ex exceção lançada
     * @return resposta HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Card não encontrado.
     */
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCardNotFound(CardNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Quantidade máxima de cartões excedida.
     */
    @ExceptionHandler(CardQuantityMaxException.class)
    public ResponseEntity<Map<String, Object>> handleCardQuantityMax(CardQuantityMaxException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Limite máximo de dispositivos suportados excedido.
     */
    @ExceptionHandler(DeviceMaxSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceMaxSupported(DeviceMaxSupportedException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Dispositivo não encontrado.
     */
    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceNotFound(DeviceNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Dispositivo não vinculado ao recurso esperado.
     */
    @ExceptionHandler(DeviceNotLinkedException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceNotLinked(DeviceNotLinkedException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Transação não encontrada.
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransactionNotFound(TransactionNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Alert Status não encontrado.
     */
    @ExceptionHandler(AlertStatusNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAlertStatusNotFoundException(AlertStatusNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Alert não encontrado.
     */
    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAlertNotFoundException(AlertNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Constrói o corpo padrão de resposta de erro.
     *
     * @param message mensagem de erro
     * @param status status HTTP
     * @return {@link ResponseEntity} padronizada
     */
    private ResponseEntity<Map<String, Object>> buildResponse(String message, HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
