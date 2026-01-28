package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.dtos.fraudalert.FraudAlertFilterRequestDTO;
import tech.safepay.services.FraudAlertService;

import java.util.UUID;

/**
 * Controller responsável pelo gerenciamento de alertas de fraude no sistema SafePay.
 * <p>
 * Exponibiliza endpoints REST para consulta paginada,
 * filtragem avançada e listagem completa de alertas
 * gerados pelo módulo antifraude.
 * </p>
 *
 * Base path: {@code /api/v1/fraud-alerts}
 *
 * @author SafePay Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/fraud-alerts")
public class FraudAlertController {

    /**
     * Serviço responsável pelas regras de negócio relacionadas
     * aos alertas de fraude.
     */
    private final FraudAlertService fraudAlertService;

    /**
     * Construtor do {@link FraudAlertController}.
     *
     * @param fraudAlertService serviço de alertas de fraude
     */
    public FraudAlertController(FraudAlertService fraudAlertService) {
        this.fraudAlertService = fraudAlertService;
    }

    /**
     * Busca alertas de fraude aplicando filtros avançados.
     * <p>
     * Permite combinar múltiplos critérios como severidade,
     * score de fraude, tipo de alerta, período de criação
     * e vínculos com transações, dispositivos e cartões.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/fraud-alerts/search?page=0&size=10}
     *
     * Body:
     * <pre>
     * {
     *   "recentAlerts": true,
     *   "severity": "HIGH",
     *   "startFraudScore": 70.0,
     *   "endFraudScore": 100.0,
     *   "alertTypeList": ["SUSPICIOUS_DEVICE", "VELOCITY_CHECK"],
     *   "createdAtFrom": "2025-01-01T00:00:00",
     *   "transactionId": "UUID",
     *   "deviceId": "UUID",
     *   "cardId": "UUID"
     * }
     * </pre>
     *
     * @param request objeto contendo os critérios de filtro
     * @param page número da página (base 0)
     * @param size quantidade de registros por página
     * @return resposta contendo página de alertas conforme filtros aplicados
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchFraudAlerts(
            @RequestBody FraudAlertFilterRequestDTO request,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "size") int size
    ) {
        return ResponseEntity.ok(
                fraudAlertService.getWithFilters(
                        request.recentAlerts(),
                        request.severity(),
                        request.startFraudScore(),
                        request.endFraudScore(),
                        request.alertTypeList(),
                        request.createdAtFrom(),
                        request.transactionId(),
                        request.deviceId(),
                        request.cardId(),
                        page,
                        size
                )
        );
    }

    /**
     * Retorna todos os alertas de fraude sem aplicação de filtros.
     * <p>
     * Endpoint auxiliar voltado para testes, debug
     * e validação de dados.
     * </p>
     *
     * Exemplo:
     * {@code GET /api/v1/fraud-alerts/list?page=0&size=10}
     *
     * @param page número da página (base 0)
     * @param size quantidade de registros por página
     * @return resposta contendo página com todos os alertas
     */
    @GetMapping("/list")
    public ResponseEntity<?> getAllFraudAlerts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                fraudAlertService.getAllWithoutFilters(page, size)
        );
    }

    /**
     * Classifica o status do alerta de maneira manual
     * <p>
     * Endpoint voltado para classificar o status do alerta baseado na análise humana
     * </p>
     *
     * Exemplo:
     * {@code GET /api/v1/fraud-alerts/status?status=2}
     *
     * @param numberStatus número referente ao status:
     * <ul>
     *     <li><code>0</code> - PENDING</li>
     *     <li><code>1</code> - CONFIRMED</li>
     *     <li><code>2</code> - FALSE_POSITIVE</li>
     * </ul>
     *  -1 < <code>numberStaus</code>  =< 2
     * @return Response de feedback
     */
    @PostMapping("/status")
    public ResponseEntity<?> classifyStatus(
            @RequestParam(name = "status") int numberStatus,
            @RequestParam(name = "transactionId") UUID transactionId
    ) {
        return ResponseEntity.ok(
                fraudAlertService.classifyStatus(numberStatus, transactionId)
        );
    }

}
