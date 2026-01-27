package tech.safepay.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.safepay.Enums.CardBrand;
import tech.safepay.services.CardService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/card")
public class CardController {
    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateCards(@RequestParam(name = "quantity") int quantity){
        return ResponseEntity.ok(cardService.cardRegister(quantity));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCards(@RequestParam(name = "id") UUID id){
        return ResponseEntity.ok(cardService.cardDelete(id));
    }

    @PutMapping("/remaining-credits/reset")
    public ResponseEntity<?> remainingCreditResetAllCards(){
        return ResponseEntity.ok(cardService.resetRemainingCreditAllCards());
    }

    @GetMapping("/get")
    public ResponseEntity<?> getWithFilters(
            @RequestParam(required = false) CardBrand cardBrand,
            @RequestParam(required = false) Boolean recentlyCreated,
            @RequestParam int size,
            @RequestParam int page
    ) {
        return ResponseEntity.ok(
                cardService.getWithFilters(cardBrand, recentlyCreated, page, size)
        );
    }
}
