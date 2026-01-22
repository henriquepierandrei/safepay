package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.CardStatus;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.cards.CardResponse;
import tech.safepay.dtos.cards.CardsInDeviceResponseDto;
import tech.safepay.entities.Card;
import tech.safepay.exceptions.card.CardQuantityMaxException;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.generator.DefaultCardGenerator;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class CardService {
    private static final Integer QUANTITY_LIMIT = 500;
    private static final Random RANDOM = new Random();

    private final DefaultCardGenerator defaultCardGenerator;
    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    public CardService(DefaultCardGenerator defaultCardGenerator, CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.defaultCardGenerator = defaultCardGenerator;
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
    }

    // Registro do cartão
    public CardResponse cardRegister(
            Integer quantity        // quantity <= QUANTITY_LIMIT
    ){

        if (cardRepository.findAll().size() >= 500){
            throw new CardQuantityMaxException("Número máximo de criações alcançados!");
        }

        for (int i = 0; i < quantity; i++) {
            Card card = new Card();

            var cardBrand = defaultCardGenerator.choiceCardBrand();

            card.setCardBrand(cardBrand);
            card.setCardNumber(defaultCardGenerator.generateNumber(cardBrand));
            card.setCreatedAt(LocalDateTime.now());
            card.setExpirationDate(defaultCardGenerator.generateExpirationDate());
            card.setRiskScore(defaultCardGenerator.generateRiskScore());

            var credit = defaultCardGenerator.generateCreditLimit();
            card.setCreditLimit(credit);
            card.setRemainingLimit(credit);


            card.setStatus(CardStatus.ACTIVE);
            card.setCardHolderName(defaultCardGenerator.generateName());
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



    /**
     * Mascara o número do cartão
     * @param cardNumber - Número do cartão para mascarar
     * @return
     */
    private String getMaskedCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Obtém uma lista de cartões vinculados ao dispositivo
     * @param deviceId Id do dispositivo
     */
    public CardsInDeviceResponseDto getCardsInDevice(UUID deviceId) {

        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Dispositivo não encontrado!"));

        var cardDtos = device.getCards().stream()
                .map(card -> new CardsInDeviceResponseDto.CardResponseDto(
                        card.getCardId(),
                        getMaskedCardNumber(card.getCardNumber()),
                        card.getCardHolderName(),
                        card.getCardBrand(),
                        card.getExpirationDate(),
                        card.getCreditLimit(),
                        card.getStatus()
                ))
                .toList();

        return new CardsInDeviceResponseDto(cardDtos);
    }


    public CardDataResponseDto getCardById(UUID cardId) {

        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Cartão não encontrado"));

        return new CardDataResponseDto(
                card.getCardId(),
                getMaskedCardNumber(card.getCardNumber()),
                card.getCardHolderName(),
                card.getCardBrand(),
                card.getExpirationDate(),
                card.getCreditLimit(),
                card.getStatus()
        );
    }




}

