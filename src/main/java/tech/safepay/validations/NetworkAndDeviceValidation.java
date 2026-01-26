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
import java.util.*;

@Component
public class NetworkAndDeviceValidation {

    private final ObjectMapper objectMapper;
    private final Set<IPAddress> vpnCidrs = new HashSet<>();

    public NetworkAndDeviceValidation(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
       REGRAS COMPARTILHADAS - SOURCE OF TRUTH
       ===================================================== */

    /**
     * Verifica se o DEVICE da transação atual é novo para este CARD.
     *
     * Um device é considerado "novo" se seu ID não aparece em nenhuma
     * transação anterior do histórico.
     */
    private boolean isNewDevice(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        Device currentDevice = transaction.getDevice();

        // Sem device = não conseguimos determinar
        if (currentDevice == null || currentDevice.getId() == null) {
            return false;
        }

        UUID currentDeviceId = currentDevice.getId();

        // Verifica se este deviceId aparece em alguma transação ANTERIOR
        boolean deviceExistsInHistory = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId())) // Exclui a atual
                .anyMatch(t -> {
                    Device d = t.getDevice();
                    return d != null && currentDeviceId.equals(d.getId());
                });

        // É novo se NÃO existe no histórico
        return !deviceExistsInHistory;
    }

    /**
     * Verifica se o FINGERPRINT da transação atual é novo para este CARD.
     *
     * Diferente de isNewDevice, aqui verificamos pelo fingerprint string,
     * não pelo ID do device.
     */
    private boolean isNewFingerprint(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        String currentFingerprint = transaction.getDeviceFingerprint();

        if (currentFingerprint == null || currentFingerprint.isBlank()) {
            return false;
        }

        // Verifica se este fingerprint aparece em alguma transação ANTERIOR
        boolean fingerprintExistsInHistory = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId()))
                .anyMatch(t -> currentFingerprint.equals(t.getDeviceFingerprint()));

        return !fingerprintExistsInHistory;
    }

    /* =====================================================
       NEW_DEVICE_DETECTED (15)

       Dispara quando: Device nunca usado com este card antes.
       Não dispara quando: Device já foi usado anteriormente.
       ===================================================== */
    public ValidationResultDto newDeviceDetected(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        // Precisa ter histórico para comparar
        boolean hasHistory = snapshot.last20().stream()
                .anyMatch(t -> !t.getTransactionId().equals(transaction.getTransactionId()));

        if (!hasHistory) {
            // Primeira transação do card - não é alerta de "novo device"
            return result;
        }

        if (isNewDevice(transaction, snapshot)) {
            result.addScore(AlertType.NEW_DEVICE_DETECTED.getScore());
            result.addAlert(AlertType.NEW_DEVICE_DETECTED);
        }

        return result;
    }

    /* =====================================================
       DEVICE_FINGERPRINT_CHANGE (25)

       Dispara quando: O MESMO device (por ID) apresenta um
                       fingerprint diferente do histórico.

       Não dispara quando:
         - Device é novo (usa NEW_DEVICE_DETECTED)
         - Fingerprint é o mesmo de antes
         - Não há transações anteriores do mesmo device

       Cenário de fraude: Alguém clonou o deviceId mas o
                          fingerprint real é diferente.
       ===================================================== */
    public ValidationResultDto deviceFingerprintChange(
            Transaction transaction,
            TransactionGlobalValidation.ValidationSnapshot snapshot
    ) {
        ValidationResultDto result = new ValidationResultDto();

        Device currentDevice = transaction.getDevice();
        String currentFingerprint = transaction.getDeviceFingerprint();

        // Validações básicas
        if (currentDevice == null || currentDevice.getId() == null) {
            return result;
        }
        if (currentFingerprint == null || currentFingerprint.isBlank()) {
            return result;
        }

        // Se é um device novo, não verifica mudança de fingerprint
        // (isso seria tratado por NEW_DEVICE_DETECTED)
        if (isNewDevice(transaction, snapshot)) {
            return result;
        }

        UUID currentDeviceId = currentDevice.getId();

        // Busca a transação mais recente do MESMO DEVICE
        Optional<Transaction> previousFromSameDevice = snapshot.last20().stream()
                .filter(t -> !t.getTransactionId().equals(transaction.getTransactionId()))
                .filter(t -> {
                    Device d = t.getDevice();
                    return d != null && currentDeviceId.equals(d.getId());
                })
                .filter(t -> t.getDeviceFingerprint() != null)
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt())); // Mais recente

        if (previousFromSameDevice.isEmpty()) {
            return result;
        }

        String previousFingerprint = previousFromSameDevice.get().getDeviceFingerprint();

        // Compara fingerprints do MESMO device
        if (!currentFingerprint.equals(previousFingerprint)) {
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

        boolean isVpn = vpnCidrs.stream().anyMatch(cidr -> cidr.contains(address));

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
        if (device == null || device.getCards() == null) return result;

        int distinctCards = device.getCards().size();
        if (distinctCards >= 3) {
            result.addScore(AlertType.MULTIPLE_CARDS_SAME_DEVICE.getScore());
            result.addAlert(AlertType.MULTIPLE_CARDS_SAME_DEVICE);
        }

        return result;
    }
}