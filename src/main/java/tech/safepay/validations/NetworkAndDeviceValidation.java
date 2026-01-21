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
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class NetworkAndDeviceValidation {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    /**
     * Lista de blocos CIDR IPv6 associados a VPNs / proxies
     */
    private final List<IPAddress> vpnIpv6Cidrs = new ArrayList<>();

    public NetworkAndDeviceValidation(
            ObjectMapper objectMapper,
            TransactionRepository transactionRepository
    ) {
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
    }

    /* =====================================================
       VPN / TOR BLACKLIST LOAD
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
                        vpnIpv6Cidrs.add(address);
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
    private boolean isNewDevice(Transaction transaction) {
        Device device = transaction.getDevice();
        if (device == null) return true;

        long totalTransactions =
                transactionRepository.countByDevice(device);

        // Se só existe a transação atual → device novo
        return totalTransactions <= 1;
    }




    /* =====================================================
       NEW_DEVICE_DETECTED (15)
       ===================================================== */
    public ValidationResultDto newDeviceDetected(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        if (isNewDevice(transaction)) {
            result.addScore(AlertType.NEW_DEVICE_DETECTED.getScore());
            result.addAlert(AlertType.NEW_DEVICE_DETECTED);
        }

        return result;
    }



    /* =====================================================
       DEVICE_FINGERPRINT_CHANGE (25)
       ===================================================== */
    public ValidationResultDto deviceFingerprintChange(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        // Regra de ouro: device novo não gera fingerprint change
        if (isNewDevice(transaction)) return result;

        transactionRepository
                .findFirstByDeviceAndTransactionIdNotOrderByCreatedAtDesc(
                        transaction.getDevice(),
                        transaction.getTransactionId()
                )
                .ifPresent(prev -> {
                    String prevFp = prev.getDeviceFingerprint();
                    String currFp = transaction.getDeviceFingerprint();

                    if (prevFp != null && currFp != null && !prevFp.equals(currFp)) {
                        result.addScore(AlertType.DEVICE_FINGERPRINT_CHANGE.getScore());
                        result.addAlert(AlertType.DEVICE_FINGERPRINT_CHANGE);
                    }
                });


        return result;
    }



    /* =====================================================
       TOR_OR_PROXY_DETECTED (35)
       ===================================================== */
    public ValidationResultDto torOrProxyDetected(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        String ip = transaction.getIpAddress();
        if (ip == null || vpnIpv6Cidrs.isEmpty()) return result;

        IPAddress address = new IPAddressString(ip).getAddress();
        if (address == null) return result;

        boolean isVpn = vpnIpv6Cidrs.stream()
                .anyMatch(cidr -> cidr.contains(address));

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
