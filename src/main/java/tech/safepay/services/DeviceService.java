package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.DeviceType;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.device.DeviceMaxSupportedException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.exceptions.device.DeviceNotLinkedException;
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

    /**
     * Gerar Dispositivo
     * @param quantity - Quantidade de Dispositivo
     * @return - Response
     */
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

            // UUID único para referenciar fingerprint
            device.setFingerPrintId(UUID.randomUUID().toString());

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

    public DeviceResponse addCardToDeviceAutomatic() {
        // Pega todos os dispositivos
        List<Device> devices = deviceRepository.findAll();

        // Se não tiver dispositivos, não faz sentido
        if (devices.isEmpty()) {
            return new DeviceResponse("Nenhum dispositivo disponível para adicionar cartões", HttpStatus.BAD_REQUEST);
        }

        // Pega dois cartões aleatórios
        List<Card> cards = sortCardToDevice();

        // Distribui os cartões nos dispositivos
        for (Device device : devices) {
            for (Card card : cards) {
                if (!device.getCards().contains(card)) {
                    device.getCards().add(card);
                }
                if (!card.getDevices().contains(device)) {
                    card.getDevices().add(device);
                }
            }
        }

        // Salva todos os dispositivos (JPA cuida do relacionamento)
        deviceRepository.saveAll(devices);
        deviceRepository.flush();

        return new DeviceResponse("Cartões adicionados automaticamente aos dispositivos", HttpStatus.OK);
    }

    ;

    /**
     * Obtém uma lista de dispositivo
     * @return - Lista de dispositivos
     */
    public Page<DeviceListResponseDto.DeviceDto> getDeviceList(
            DeviceType deviceType,
            String os,
            String browser,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Device> devices = deviceRepository.findWithFilters(
                deviceType,
                os,
                browser,
                pageable
        );

        return devices.map(device ->
                new DeviceListResponseDto.DeviceDto(
                        device.getId(),
                        device.getFingerPrintId(),
                        device.getDeviceType(),
                        device.getOs(),
                        device.getBrowser()
                )
        );
    }


    public DeviceResponse removeCardInDevice(UUID cardId, UUID deviceId) {
        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Cartão de crédito não encontrado!"));

        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Dispositivo não encontrado!"));

        if (!card.getDevices().contains(device)) {
            throw new DeviceNotLinkedException("Dispositivo não vinculado a este cartão");
        }

        card.getDevices().remove(device);
        cardRepository.save(card);

        return new DeviceResponse(
                "Cartão removido do dispositivo!",
                HttpStatus.OK
        );
    }

    public DeviceResponse updateFingerPrint(UUID deviceId) {
        var optionalDevice = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException("Não foi possível encontrar o dispositivo."));
        UUID newFingerPrint = UUID.randomUUID();

        optionalDevice.setFingerPrintId(newFingerPrint.toString());
        deviceRepository.save(optionalDevice);
        return new DeviceResponse(
                "Biometria atualizada com sucesso!",
                HttpStatus.OK
        );

    }

    public record DeviceResponse(String message, HttpStatus status) {
    }

    /**
     * Adiciona cartão no dispositivo
     *
     * @param cardId   - Id do cartão
     * @param deviceId - Id do dispositivo
     */
    public record AddCardDto(UUID cardId, UUID deviceId) {
    }


}
