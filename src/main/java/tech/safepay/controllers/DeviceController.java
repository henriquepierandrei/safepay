package tech.safepay.controllers;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.Enums.DeviceType;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.services.CardService;
import tech.safepay.services.DeviceService;

import java.util.UUID;

/**
 * Controller responsável pelo gerenciamento de dispositivos no sistema SafePay.
 * <p>
 * Disponibiliza endpoints REST para geração de dispositivos,
 * listagem paginada com filtros, vínculo e desvínculo de cartões
 * e atualização de fingerprint.
 * </p>
 *
 * Base path: {@code /api/v1/device}
 *
 * @author SafePay Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1")
public class DeviceController {

    /**
     * Serviço responsável pelas regras de negócio relacionadas a dispositivos.
     */
    private final DeviceService deviceService;

    /**
     * Serviço responsável pelas regras de negócio relacionadas a cartões.
     */
    private final CardService cardService;

    /**
     * Construtor do {@link DeviceController}.
     *
     * @param deviceService serviço de dispositivos
     * @param cardService serviço de cartões
     */
    public DeviceController(DeviceService deviceService, CardService cardService) {
        this.deviceService = deviceService;
        this.cardService = cardService;
    }

    /**
     * Gera dispositivos simulados para ambiente de teste.
     * <p>
     * Utilizado para popular o sistema em ambientes de desenvolvimento
     * e homologação.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/device/generate?quantity=10}
     *
     * @param quantity quantidade de dispositivos a serem gerados
     * @return resposta contendo a lista de dispositivos gerados
     */
    @PostMapping("/admin/device/generate")
    public ResponseEntity<?> generateDevices(
            @RequestParam(name = "quantity") int quantity
    ) {
        return ResponseEntity.ok(deviceService.generateDevice(quantity));
    }

    /**
     * Retorna uma lista paginada de dispositivos cadastrados,
     * com filtros opcionais.
     *
     * Exemplo:
     * {@code GET /api/v1/device/list?deviceType=POS&os=Linux&browser=Chrome&page=0&size=10}
     *
     * @param deviceType filtro opcional por tipo de dispositivo
     * @param os filtro opcional por sistema operacional
     * @param browser filtro opcional por navegador
     * @param page número da página (base 0)
     * @param size quantidade de registros por página
     * @return página de dispositivos conforme filtros aplicados
     */
    @GetMapping("/device/list")
    public ResponseEntity<Page<DeviceListResponseDto.DeviceDto>> getDeviceList(
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) String os,
            @RequestParam(required = false) String browser,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return ResponseEntity.ok(
                deviceService.getDeviceList(deviceType, os, browser, page, size)
        );
    }

    /**
     * Vincula manualmente um cartão a um dispositivo específico.
     *
     * Exemplo:
     * {@code POST /api/v1/device/cards/add}
     *
     * Body:
     * <pre>
     * {
     *   "deviceId": "UUID",
     *   "cardId": "UUID"
     * }
     * </pre>
     *
     * @param dto objeto contendo os identificadores do dispositivo e do cartão
     * @return resposta contendo o resultado da operação
     */
    @PostMapping("/cards/add")
    public ResponseEntity<?> addCard(
            @RequestBody DeviceService.AddCardDto dto
    ) {
        return ResponseEntity.ok(deviceService.addCardToDevice(dto));
    }

    /**
     * Vincula automaticamente um cartão disponível a um dispositivo.
     * <p>
     * A lógica de seleção do cartão é totalmente delegada ao service.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/device/cards/add/automatic}
     *
     * @return resposta contendo o resultado da operação
     */
    @PostMapping("/admin/device/cards/add/automatic")
    public ResponseEntity<?> addCardAutomatic() {
        return ResponseEntity.ok(deviceService.addCardToDeviceAutomatic());
    }

    /**
     * Remove o vínculo entre um cartão e um dispositivo.
     *
     * Exemplo:
     * {@code DELETE /api/v1/device/cards/remove?cardId=UUID&deviceId=UUID}
     *
     * @param cardId identificador único do cartão
     * @param deviceId identificador único do dispositivo
     * @return resposta confirmando a remoção do vínculo
     */
    @DeleteMapping("/cards/remove")
    public ResponseEntity<?> removeCard(
            @RequestParam(name = "cardId") UUID cardId,
            @RequestParam(name = "deviceId") UUID deviceId
    ) {
        return ResponseEntity.ok(
                deviceService.removeCardInDevice(cardId, deviceId)
        );
    }

    /**
     * Retorna todos os cartões vinculados a um dispositivo específico.
     *
     * Exemplo:
     * {@code GET /api/v1/device/cards/get/list?deviceId=UUID}
     *
     * @param deviceId identificador único do dispositivo
     * @return lista de cartões associados ao dispositivo
     */
    @GetMapping("/cards/get/list")
    public ResponseEntity<?> getCardListInDevice(
            @RequestParam(name = "deviceId") UUID deviceId
    ) {
        return ResponseEntity.ok(cardService.getCardsInDevice(deviceId));
    }

    /**
     * Atualiza o fingerprint de um dispositivo.
     * <p>
     * Geralmente utilizado para simular mudança de ambiente
     * ou reforçar mecanismos antifraude.
     * </p>
     *
     * Exemplo:
     * {@code PUT /api/v1/device/fingerprint/update?deviceId=UUID}
     *
     * @param deviceId identificador único do dispositivo
     * @return resposta contendo o novo fingerprint
     */
    @PutMapping("/fingerprint/update")
    public ResponseEntity<?> updateFingerPrint(
            @RequestParam(name = "deviceId", required = true) UUID deviceId
    ) {
        return ResponseEntity.ok(deviceService.updateFingerPrint(deviceId));
    }
}
