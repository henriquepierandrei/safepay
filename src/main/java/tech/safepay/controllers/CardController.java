package tech.safepay.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.safepay.services.CardService;

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
}
