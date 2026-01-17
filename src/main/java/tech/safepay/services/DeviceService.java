package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.Enums.DeviceType;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.device.DeviceMaxSupportedException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class DeviceService {
    private static final int MAX_DEVICE_SUPPORTED = 20;
    private static final Random RANDOM = new Random();

    private static final String[] OS_OPTIONS_DESKTOP = {"Windows 10", "Windows 11", "Linux", "macOS"};
    private static final String[] OS_OPTIONS_MOBILE = {"Android", "iOS"};
    private static final String[] DESKTOP_BROWSERS = {"Chrome", "Firefox", "Edge", "Safari"};
    private static final String[] MOBILE_BROWSERS  = {"Chrome Mobile", "Safari Mobile", "Samsung Internet"};

    public record DeviceResponse(String message, HttpStatus status){}

    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    public DeviceService(CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
    }

    // Pega dois cartões aleatórios
    public List<Card> sortCardToDevice() {
        List<Card> cards = new ArrayList<>(cardRepository.findAll());
        if (cards.size() < 2) {
            throw new IllegalStateException("Not enough cards to associate with device");
        }
        Collections.shuffle(cards);
        return cards.subList(0, 2);
    }

    public DeviceResponse generateDevice(int quantity) {
        long atualQuantity = deviceRepository.count();

        if (atualQuantity + quantity > MAX_DEVICE_SUPPORTED) {
            throw new DeviceMaxSupportedException(
                    "Você pode registrar somente " + (MAX_DEVICE_SUPPORTED - atualQuantity) + " dispositivos!"
            );
        }

        List<Device> devicesToSave = new ArrayList<>();

        for (int i = 0; i < quantity; i++) {
            Device device = new Device();

            // UUID único
            device.setDeviceId(UUID.randomUUID().toString());

            // Cartões
            device.setCards(sortCardToDevice());

            // Tipo de dispositivo aleatório
            DeviceType[] types = DeviceType.values();
            DeviceType type = types[RANDOM.nextInt(types.length)];
            device.setDeviceType(type);

            // OS e Browser
            device.setOs(randomOs(type));
            device.setBrowser(randomBrowser(type));

            // Timestamps
            Instant now = Instant.now();
            device.setFirstSeenAt(now);
            device.setLastSeenAt(now);

            devicesToSave.add(device);
        }

        // Salva tudo de uma vez
        deviceRepository.saveAll(devicesToSave);
        deviceRepository.flush();

        return new DeviceResponse("Registro bem sucedido", HttpStatus.OK);
    }

    // Auxiliares para random OS e Browser
    private String randomOs(DeviceType type) {
        return switch (type) {
            case DESKTOP -> OS_OPTIONS_DESKTOP[RANDOM.nextInt(OS_OPTIONS_DESKTOP.length)];
            case MOBILE -> OS_OPTIONS_MOBILE[RANDOM.nextInt(OS_OPTIONS_MOBILE.length)];
            default -> "Embedded Linux";
        };
    }

    private String randomBrowser(DeviceType type) {
        return switch (type) {
            case DESKTOP -> DESKTOP_BROWSERS[RANDOM.nextInt(DESKTOP_BROWSERS.length)];
            case MOBILE -> MOBILE_BROWSERS[RANDOM.nextInt(MOBILE_BROWSERS.length)];
            default -> "N/A";
        };
    }



    public record  AddCardDto(UUID cardId, UUID deviceId){};

    public DeviceResponse addCardToDevice(AddCardDto dto){
        var card = cardRepository.findById(dto.cardId())
                .orElseThrow(() -> new CardNotFoundException("Cartão de crédito não encontrado!"));
        var device = deviceRepository.findById(dto.deviceId())
                .orElseThrow(() -> new DeviceNotFoundException("Dispositivo não encontrado!"));

        // Evita duplicatas
        if (!device.getCards().contains(card)) {
            device.getCards().add(card);
        }

        if (!card.getDevices().contains(device)) {
            card.getDevices().add(device);
        }

        // Salva apenas um dos lados, JPA vai cuidar do relacionamento
        deviceRepository.save(device);

        return new DeviceResponse("Cartão adicionado no dispositivo", HttpStatus.OK);
    }


}
