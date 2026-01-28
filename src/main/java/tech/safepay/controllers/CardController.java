package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.Enums.CardBrand;
import tech.safepay.dtos.cards.CardBlockResponseDto;
import tech.safepay.services.CardService;

import java.util.UUID;

/**
 * Controller responsável pelo gerenciamento de cartões no sistema SafePay.
 * <p>
 * Fornece endpoints REST para operações de criação, exclusão,
 * reset de crédito e listagem paginada de cartões com filtros opcionais.
 * </p>
 *
 * Base path: {@code /api/v1/card}
 *
 * @author SafePay Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/card")
public class CardController {

    /**
     * Serviço responsável pelas regras de negócio relacionadas a cartões.
     */
    private final CardService cardService;

    /**
     * Construtor do {@link CardController}.
     *
     * @param cardService serviço de cartões injetado pelo Spring
     */
    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * Gera cartões simulados para ambiente de teste.
     * <p>
     * Utilizado principalmente em ambientes de desenvolvimento e homologação.
     * </p>
     *
     * Exemplo:
     * {@code POST /api/v1/card/generate?quantity=10}
     *
     * @param quantity quantidade de cartões a serem gerados
     * @return resposta contendo a lista de cartões gerados
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateCards(
            @RequestParam(name = "quantity") int quantity
    ) {
        return ResponseEntity.ok(cardService.cardRegister(quantity));
    }

    /**
     * Remove um cartão específico do sistema a partir do seu identificador.
     *
     * Exemplo:
     * {@code DELETE /api/v1/card/delete?id=550e8400-e29b-41d4-a716-446655440000}
     *
     * @param id identificador único (UUID) do cartão
     * @return resposta confirmando a exclusão do cartão
     */
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCard(
            @RequestParam(name = "id") UUID id
    ) {
        return ResponseEntity.ok(cardService.cardDelete(id));
    }

    /**
     * Reseta o crédito disponível de todos os cartões cadastrados.
     * <p>
     * Endpoint utilitário para reinicialização de cenários de teste.
     * </p>
     *
     * Exemplo:
     * {@code PUT /api/v1/card/remaining-credits/reset}
     *
     * @return resposta confirmando o reset dos créditos
     */
    @PutMapping("/remaining-credits/reset")
    public ResponseEntity<?> resetRemainingCredits() {
        return ResponseEntity.ok(cardService.resetRemainingCreditAllCards());
    }

    /**
     * Retorna uma lista paginada de cartões com filtros opcionais.
     *
     * Exemplo:
     * {@code GET /api/v1/card/list?cardBrand=VISA&recentlyCreated=true&page=0&size=10}
     *
     * @param cardBrand filtro opcional por bandeira do cartão
     * @param recentlyCreated filtro opcional para cartões criados recentemente
     * @param page número da página (base 0)
     * @param size quantidade de registros por página
     * @return resposta contendo página de cartões conforme filtros aplicados
     */
    @GetMapping("/list")
    public ResponseEntity<?> getCardList(
            @RequestParam(required = false) CardBrand cardBrand,
            @RequestParam(required = false) Boolean recentlyCreated,
            @RequestParam int page,
            @RequestParam int size
    ) {
        return ResponseEntity.ok(
                cardService.getWithFilters(cardBrand, recentlyCreated, page, size)
        );
    }

    /**
     * Endpoint para bloquear ou marcar um cartão como perdido com base no ID do cartão e do dispositivo.
     *
     * <p>O cartão só será atualizado se o {@code deviceId} corresponder ao dispositivo vinculado
     * ao cartão e o {@code cardId} corresponder ao cartão informado. Dependendo do endpoint:
     * </p>
     * <ul>
     *     <li><b>/block</b> - Marca o cartão como bloqueado.</li>
     *     <li><b>/lost</b> - Marca o cartão como perdido.</li>
     * </ul>
     *
     * <p>Essa operação previne o uso indevido de cartões, garantindo que apenas combinações
     * válidas de dispositivo e cartão possam ser alteradas.</p>
     *
     * @param cardId UUID do cartão a ser atualizado
     * @param deviceId UUID do dispositivo vinculado ao cartão
     * @return {@link CardBlockResponseDto} contendo o resultado da operação
     */

    @PostMapping("/block")
    public ResponseEntity<CardBlockResponseDto> blockCard(@RequestParam UUID cardId, @RequestParam UUID deviceId) {
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, deviceId, true));
    }
    @PostMapping("/lost")
    public ResponseEntity<CardBlockResponseDto> markCardAsLost(@RequestParam UUID cardId, @RequestParam UUID deviceId) {
        return ResponseEntity.ok(cardService.updateCardStatus(cardId, deviceId, false));
    }



}


