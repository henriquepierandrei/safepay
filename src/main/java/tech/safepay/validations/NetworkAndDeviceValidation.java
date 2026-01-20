package tech.safepay.validations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.AlertType;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class NetworkAndDeviceValidation {

    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    /**
     * Lista de blocos CIDR IPv6 associados a provedores de VPN,
     * proxies e infraestrutura de datacenter.
     *
     * Carregada uma única vez no startup da aplicação.
     */
    private final List<SubnetUtils.SubnetInfo> vpnIpv6Cidrs = new ArrayList<>();

    public NetworkAndDeviceValidation(
            ObjectMapper objectMapper,
            TransactionRepository transactionRepository
    ) {
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
    }

    /**
     * =========================
     * VPN / TOR BLACKLIST LOAD
     * =========================
     *
     * Objetivo:
     * Carregar, em memória, a blacklist de redes IPv6 associadas
     * a VPNs, proxies e datacenters.
     *
     * Fonte:
     * Arquivo JSON versionado em resources/data,
     * baseado em dados open-source (OpenStreetMap / Threat Intel).
     *
     * Estratégia:
     * - Executado no startup (@PostConstruct)
     * - Converte CIDRs em SubnetInfo para lookup rápido
     *
     * Fail-safe:
     * - Em caso de erro, a lista é esvaziada
     * - O sistema continua operando sem esse sinal
     */
    @PostConstruct
    private void loadVpnBlacklist() {
        try {
            ClassPathResource resource =
                    new ClassPathResource("data/vpn-ipv6-blacklist.json");

            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode list = root.get("list");

                for (JsonNode cidr : list) {
                    SubnetUtils subnet = new SubnetUtils(cidr.asText());
                    subnet.setInclusiveHostCount(true);
                    vpnIpv6Cidrs.add(subnet.getInfo());
                }
            }
        } catch (Exception ignored) {
            vpnIpv6Cidrs.clear();
        }
    }

    /**
     * =========================
     * NEW_DEVICE_DETECTED (15)
     * =========================
     *
     * Objetivo:
     * Identificar quando um cartão está sendo utilizado
     * em um device nunca associado a ele anteriormente.
     *
     * Estratégia:
     * - Compara o device da transação com a lista de devices já vinculados ao cartão
     *
     * Regra:
     * - Device não presente no histórico do cartão → sinal acionado
     *
     * Observações:
     * - Sinal fraco isoladamente
     * - Muito eficiente quando combinado com fingerprint change ou VPN
     */
    public int newDeviceDetected(Transaction transaction) {

        Card card = transaction.getCard();
        Device device = transaction.getDevice();

        if (card == null || device == null) {
            return 0;
        }

        boolean alreadyUsed =
                card.getDevices()
                        .stream()
                        .anyMatch(d -> d.getId().equals(device.getId()));

        return alreadyUsed
                ? 0
                : AlertType.NEW_DEVICE_DETECTED.getScore();
    }

    /**
     * =========================
     * DEVICE_FINGERPRINT_CHANGE (25)
     * =========================
     *
     * Objetivo:
     * Detectar alteração de identidade biométrica entre
     * transações consecutivas do mesmo cartão.
     *
     * Interpretação do fingerprint:
     * O fingerPrintId representa uma identidade biométrica única
     * (ex: impressão digital simulada).
     *
     * Estratégia:
     * - Recupera a última transação do cartão
     * - Compara o fingerprint do device anterior com o atual
     *
     * Regra:
     * - Fingerprint diferente da transação anterior → sinal acionado
     *
     * Observações:
     * - Não dispara na primeira transação
     * - Sinal de força média
     * - Altamente relevante quando combinado com velocity ou VPN
     */
    public int deviceFingerprintChange(Transaction transaction) {

        Card card = transaction.getCard();
        Device currentDevice = transaction.getDevice();

        if (card == null || currentDevice == null) {
            return 0;
        }

        return transactionRepository
                .findFirstByCardOrderByCreatedAtDesc(card)
                .filter(previousTransaction ->
                        previousTransaction.getDevice() != null &&
                                !previousTransaction.getDevice()
                                        .getFingerPrintId()
                                        .equals(currentDevice.getFingerPrintId())
                )
                .map(t -> AlertType.DEVICE_FINGERPRINT_CHANGE.getScore())
                .orElse(0);
    }

    /**
     * =========================
     * TOR_OR_PROXY_DETECTED (35)
     * =========================
     *
     * Objetivo:
     * Detectar uso de VPN, proxy ou infraestrutura mascarada
     * durante a transação.
     *
     * Estratégia:
     * - Verifica se o IP da transação pertence a algum
     *   bloco CIDR listado como VPN/datacenter
     *
     * Regra:
     * - IP dentro de range blacklist → sinal acionado
     *
     * Observações:
     * - Forte indicativo de fraude quando combinado com outros sinais
     * - Não bloqueia sozinho
     */
    public int torOrProxyDetected(Transaction transaction) {

        String ip = transaction.getIpAddress();

        if (ip == null || vpnIpv6Cidrs.isEmpty()) {
            return 0;
        }

        boolean isVpn =
                vpnIpv6Cidrs.stream()
                        .anyMatch(subnet -> subnet.isInRange(ip));

        return isVpn
                ? AlertType.TOR_OR_PROXY_DETECTED.getScore()
                : 0;
    }

    /**
     * =========================
     * MULTIPLE_CARDS_SAME_DEVICE (50)
     * =========================
     *
     * Objetivo:
     * Detectar uso do mesmo device (fingerprint físico)
     * para múltiplos cartões distintos.
     *
     * Estratégia:
     * - Conta quantos cartões diferentes estão associados ao device
     *
     * Regra:
     * - Device utilizado por 3 ou mais cartões → sinal acionado
     *
     * Observações:
     * - Indicador clássico de fraude em escala
     * - Um dos sinais mais fortes do motor antifraude
     */
    public int multipleCardsSameDevice(Transaction transaction) {

        Device device = transaction.getDevice();

        if (device == null) {
            return 0;
        }

        int distinctCards = device.getCards().size();

        return distinctCards >= 3
                ? AlertType.MULTIPLE_CARDS_SAME_DEVICE.getScore()
                : 0;
    }
}
