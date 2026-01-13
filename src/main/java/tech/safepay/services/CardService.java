package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.CardStatus;
import tech.safepay.dtos.cards.CardResponse;
import tech.safepay.entities.Card;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.generator.DefaultCardGenerator;
import tech.safepay.repositories.CardRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class CardService {
    private static final Integer QUANTITY_LIMIT = 50;
    private static final Random RANDOM = new Random();

    private final DefaultCardGenerator defaultCardGenerator;
    private final CardRepository cardRepository;

    public CardService(DefaultCardGenerator defaultCardGenerator, CardRepository cardRepository) {
        this.defaultCardGenerator = defaultCardGenerator;
        this.cardRepository = cardRepository;
    }

    // Registro do cartão
    public CardResponse cardRegister(
            Integer quantity        // quantity <= QUANTITY_LIMIT
    ){

        for (int i = 0; i < quantity; i++) {
            Card card = new Card();

            var cardBrand = defaultCardGenerator.choiceCardBrand();

            card.setCardBrand(cardBrand);
            card.setCardNumber(defaultCardGenerator.generateNumber(cardBrand));
            card.setCardHolderName("Customer SafePay");
            card.setCreatedAt(LocalDateTime.now());
            card.setExpirationDate(defaultCardGenerator.generateExpirationDate());
            card.setRiskScore(defaultCardGenerator.generateRiskScore());
            card.setCreditLimit(defaultCardGenerator.generateCreditLimit());
            card.setStatus(CardStatus.ACTIVE);

            cardRepository.saveAndFlush(card);

        }

        return new CardResponse(
                HttpStatus.CREATED,
                "Cartões criados"
        );
    }


    // Deletar um cartão
    public CardResponse cardDelete(UUID id){
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException("Cartão não encontrado"));
        cardRepository.delete(card);
        return new CardResponse(
                HttpStatus.OK,
                "Cartão deletado com sucesso!"
        );
    }
}

