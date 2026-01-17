package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.Enums.DeviceType;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;

import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class DeviceService {

    private static final Random RANDOM = new Random();

    public record DeviceResponse(String message, HttpStatus status){}


    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    public DeviceService(CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
    }


    public List<Card> sortCardToDevice() {
        List<Card> cards = new ArrayList<>(cardRepository.findAll());

        if (cards.size() < 2) {
            throw new IllegalStateException("Not enough cards to associate with device");
        }

        Collections.shuffle(cards);

        return cards.subList(0, 2);
    }

    public DeviceResponse generateDevice() {
        Device device = new Device();

        // UUID único
        device.setDeviceId(UUID.randomUUID().toString());

        device.setCards(sortCardToDevice());

        // Tipo de dispositivo aleatório
        DeviceType[] types = DeviceType.values();
        var devi = types[RANDOM.nextInt(types.length)];
        device.setDeviceType(devi);

        String[] osOptionsDesktop = {"Windows 10", "Windows 11", "Linux", "macOS"};
        String[] osOptionsMobile = {"Android", "iOS"};

        if (devi.equals(DeviceType.DESKTOP)){
            device.setOs(osOptionsDesktop[RANDOM.nextInt(osOptionsDesktop.length)]);
        } else if (devi.equals(DeviceType.MOBILE)) {
            device.setOs(osOptionsMobile[RANDOM.nextInt(osOptionsMobile.length)]);
        } else {
            // TERMINAL ou outros
            device.setOs("Embedded Linux");
        }



        // Browser aleatório
        String[] desktopBrowsers = {"Chrome", "Firefox", "Edge", "Safari"};
        String[] mobileBrowsers  = {"Chrome Mobile", "Safari Mobile", "Samsung Internet"};

        if (devi.equals(DeviceType.DESKTOP)) {
            device.setBrowser(desktopBrowsers[RANDOM.nextInt(desktopBrowsers.length)]);
        } else if (devi.equals(DeviceType.MOBILE)) {
            device.setBrowser(mobileBrowsers[RANDOM.nextInt(mobileBrowsers.length)]);
        } else {
            device.setBrowser("N/A");
        }


        // Timestamps
        Instant now = Instant.now();
        device.setFirstSeenAt(now);
        device.setLastSeenAt(now);

        // Aqui você poderia salvar no banco se tiver DeviceRepository
        // deviceRepository.save(device);

        return new DeviceResponse("Device generated: " + device.getDeviceId(), HttpStatus.OK);
    }
}
