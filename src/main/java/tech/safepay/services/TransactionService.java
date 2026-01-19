package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.exceptions.transaction.TransactionNotFoundException;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.util.UUID;

@Service
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    public TransactionService(TransactionRepository transactionRepository, CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
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





    public TransactionResponseDto getTransactionById(UUID id) {
        var transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

        var card = transaction.getCard();
        var device = transaction.getDevice();

        var cardDto = new CardDataResponseDto(
                card.getCardId(),
                getMaskedCardNumber(card.getCardNumber()),
                card.getCardHolderName(),
                card.getCardBrand(),
                card.getExpirationDate(),
                card.getCreditLimit(),
                card.getStatus()
        );

        var deviceDto = new DeviceListResponseDto.DeviceDto(
                device.getId(),
                device.getFingerPrintId(),
                device.getDeviceType(),
                device.getOs(),
                device.getBrowser()
        );

        return new TransactionResponseDto(
                cardDto,
                transaction.getMerchantCategory(),
                transaction.getAmount(),
                transaction.getTransactionDateAndTime(),
                transaction.getLatitude(),
                transaction.getLongitude(),
                deviceDto,
                transaction.getIpAddress(),
                transaction.getStatus(),
                transaction.getFraud(),
                transaction.getCreatedAt()
        );
    }

}
