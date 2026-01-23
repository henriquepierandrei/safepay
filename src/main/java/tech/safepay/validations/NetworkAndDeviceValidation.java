package tech.safepay.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NetworkAndDeviceValidation {

    private final ObjectMapper objectMapper;

    /**
     * Set otimizado para lookup de VPN / TOR (IPv6 CIDR)
     */
    private final Set<IPAddress> vpnCidrs = new HashSet<>();

    public NetworkAndDeviceValidation(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    /* =====================================================
       VPN / TOR BLACKLIST LOAD (STARTUP)
       ===================================================== */
    @PostConstruct
    private void loadVpnBlacklist() {
        try {
            ClassPathResource resource =
                    new ClassPathResource("data/vpn-ipv6-blacklist.json");

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode list = root.get("list");

                for (JsonNode cidr : list) {
                    IPAddress address =
                            new IPAddressString(cidr.asText()).getAddress();
                    if (address != null) {
                        vpnCidrs.add(address);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load VPN IPv6 blacklist", e
            );
        }
    }

    /* =====================================================
       SHARED RULE – SOURCE OF TRUTH
       ===================================================== */
    private boolean isNewDevice(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        List<Transaction> history = snapshot.last20();
        return history == null || history.size() <= 1;
    }

    /* =====================================================
       NEW_DEVICE_DETECTED (15)
       ===================================================== */
    public ValidationResultDto newDeviceDetected(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        if (isNewDevice(transaction, snapshot)) {
            result.addScore(AlertType.NEW_DEVICE_DETECTED.getScore());
            result.addAlert(AlertType.NEW_DEVICE_DETECTED);
        }

        return result;
    }

    /* =====================================================
       DEVICE_FINGERPRINT_CHANGE (25)
       ===================================================== */
    public ValidationResultDto deviceFingerprintChange(Transaction transaction, TransactionGlobalValidation.ValidationSnapshot snapshot) {
        ValidationResultDto result = new ValidationResultDto();

        if (isNewDevice(transaction, snapshot)) return result;

        List<Transaction> history = snapshot.last20();
        if (history.size() < 2) return result;

        // pega a transação mais recente antes da atual
        Transaction previous = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // do mais recente pro mais antigo
                .findFirst()
                .orElse(null);


        if (previous == null) return result;

        String prevFp = previous.getDeviceFingerprint();
        String currFp = transaction.getDeviceFingerprint();

        if (prevFp != null && currFp != null && !prevFp.equals(currFp)) {
            result.addScore(AlertType.DEVICE_FINGERPRINT_CHANGE.getScore());
            result.addAlert(AlertType.DEVICE_FINGERPRINT_CHANGE);
        }

        return result;
    }


    /* =====================================================
       TOR_OR_PROXY_DETECTED (35)
       ===================================================== */
    public ValidationResultDto torOrProxyDetected(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        String ip = transaction.getIpAddress();
        if (ip == null || vpnCidrs.isEmpty()) return result;

        IPAddress address = new IPAddressString(ip).getAddress();
        if (address == null) return result;

        boolean isVpn =
                vpnCidrs.stream().anyMatch(cidr -> cidr.contains(address));

        if (isVpn) {
            result.addScore(AlertType.TOR_OR_PROXY_DETECTED.getScore());
            result.addAlert(AlertType.TOR_OR_PROXY_DETECTED);
        }

        return result;
    }

    /* =====================================================
       MULTIPLE_CARDS_SAME_DEVICE (50)
       ===================================================== */
    public ValidationResultDto multipleCardsSameDevice(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Device device = transaction.getDevice();
        if (device == null) return result;

        int distinctCards = device.getCards().size();
        if (distinctCards >= 3) {
            result.addScore(AlertType.MULTIPLE_CARDS_SAME_DEVICE.getScore());
            result.addAlert(AlertType.MULTIPLE_CARDS_SAME_DEVICE);
        }

        return result;
    }
}
