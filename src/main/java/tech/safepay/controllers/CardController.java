package tech.safepay.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
}
