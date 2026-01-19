package tech.safepay.controllers;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.services.CardService;
import tech.safepay.services.DeviceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {
    private final DeviceService deviceService;
    private final CardService cardService;

    public DeviceController(DeviceService deviceService, CardService cardService) {
        this.deviceService = deviceService;
        this.cardService = cardService;
    }

    /**
     * Gera dispositivos simulados.
     * Usado para ambiente de teste / simulação.
     *
     * Exemplo:
     * POST /api/v1/device/generate?quantity=10
     *
     * @param quantity quantidade de dispositivos a serem gerados
     * @return lista de dispositivos gerados
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateDevices(
            @RequestParam(name = "quantity") int quantity
    ) {
        // Delegação total da lógica para o service
        return ResponseEntity.ok(deviceService.generateDevice(quantity));
    }

    /**
     * Retorna a lista de todos os dispositivos cadastrados.
     *
     * Exemplo:
     * GET /api/v1/device/list
     *
     * @return lista de dispositivos (DTO)
     */
    @GetMapping("/list")
    public ResponseEntity<?> getDeviceList() {
        // Controller não transforma dados, apenas repassa
        return ResponseEntity.ok(deviceService.getDeviceList());
    }

    /**
     * Vincula um cartão a um dispositivo.
     *
     * Exemplo:
     * POST /api/v1/device/cards/add
     * Body:
     * {
     *   "deviceId": "...",
     *   "cardId": "..."
     * }
     *
     * @param dto objeto contendo os IDs necessários para o vínculo
     * @return resultado da operação
     */
    @PostMapping("/cards/add")
    public ResponseEntity<?> add(
            @RequestBody DeviceService.AddCardDto dto
    ) {
        // Valida e processa o vínculo no service
        return ResponseEntity.ok(deviceService.addCardToDevice(dto));
    }

    /**
     * Retorna todos os cartões vinculados a um dispositivo específico.
     *
     * Exemplo:
     * GET /api/v1/device/cards/get/list?deviceId=UUID
     *
     * @param deviceId identificador técnico do dispositivo
     * @return lista de cartões vinculados ao dispositivo
     */
    @GetMapping("/cards/get/list")
    public ResponseEntity<?> getCardListInDevice(
            @RequestParam(name = "deviceId") UUID deviceId
    ) {
        // Busca cartões associados ao device
        return ResponseEntity.ok(cardService.getCardsInDevice(deviceId));
    }

}