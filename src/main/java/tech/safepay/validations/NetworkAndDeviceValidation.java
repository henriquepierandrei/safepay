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

/**
 * Componente responsável por validações relacionadas a rede e dispositivos.
 * <p>
 * Esta classe implementa mecanismos de detecção de anomalias relacionadas ao uso
 * de dispositivos e redes, incluindo identificação de dispositivos novos, mudanças
 * de fingerprint, uso de proxies/VPN/Tor, e padrões suspeitos de múltiplos cartões
 * no mesmo dispositivo.
 * </p>
 * <p>
 * As validações incluem:
 * <ul>
 *   <li>Detecção de novo dispositivo (NEW_DEVICE_DETECTED)</li>
 *   <li>Detecção de mudança de fingerprint (DEVICE_FINGERPRINT_CHANGE)</li>
 *   <li>Detecção de Tor/Proxy/VPN (TOR_OR_PROXY_DETECTED)</li>
 *   <li>Detecção de múltiplos cartões no mesmo dispositivo (MULTIPLE_CARDS_SAME_DEVICE)</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class NetworkAndDeviceValidation {

    /**
     * Threshold mínimo de cartões diferentes no mesmo dispositivo para sinalizar comportamento suspeito.
     */
    private static final int MULTIPLE_CARDS_THRESHOLD = 4;

    /**
     * Mapper para processamento de arquivos JSON.
     */
    private final ObjectMapper objectMapper;

    /**
     * Conjunto de endereços IP (CIDR) conhecidos de VPN/Proxy/Tor em formato IPv6.
     * <p>
     * Este conjunto é carregado na inicialização a partir do arquivo de blacklist
     * e utilizado para detecção de conexões através de redes anônimas.
     * </p>
     */
    private final Set<IPAddress> vpnCidrs = new HashSet<>();

    /**
     * Construtor para injeção de dependências.
     *
     * @param objectMapper mapper JSON para processamento de arquivos de configuração
     */
    public NetworkAndDeviceValidation(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Carrega a blacklist de endereços IP de VPN/Proxy/Tor na inicialização do componente.
     * <p>
     * Este método é executado automaticamente após a construção do bean, carregando
     * o arquivo {@code vpn-ipv6-blacklist.json} do classpath e populando o conjunto
     * de CIDRs bloqueados.
     * </p>
     * <p>
     * <strong>Formato do arquivo esperado:</strong>
     * <pre>
     * {
     *   "list": [
     *     "2001:db8::/32",
     *     "2001:0db8:85a3::/48",
     *     ...
     *   ]
     * }
     * </pre>
     * </p>
     *
     * @throws IllegalStateException se ocorrer erro ao carregar ou processar o arquivo
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
     * Verifica se o dispositivo da transação atual é novo para o cartão.
     * <p>
     * Um dispositivo é considerado "novo" se seu ID não aparece em nenhuma
     * transação anterior do histórico do cartão. Esta verificação é fundamental
     * para identificar o primeiro uso de um dispositivo específico.
     * </p>
     * <p>
     * <strong>Lógica de Detecção:</strong>
     * <ol>
     *   <li>Extrai o ID do dispositivo da transação atual</li>
     *   <li>Percorre o histórico de transações (excluindo a atual)</li>
     *   <li>Verifica se o ID do dispositivo aparece em alguma transação anterior</li>
     *   <li>Retorna {@code true} se não houver ocorrências prévias</li>
     * </ol>
     * </p>
     *
     * @param transaction a transação atual sendo analisada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@code true} se o dispositivo nunca foi usado com este cartão antes,
     *         {@code false} se o dispositivo já existe no histórico ou se os dados
     *         do dispositivo não estiverem disponíveis
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
     * Verifica se o fingerprint do dispositivo da transação atual é novo para o cartão.
     * <p>
     * Diferente de {@link #isNewDevice(Transaction, TransactionGlobalValidation.ValidationSnapshot)},
     * este método verifica pelo fingerprint string (hash único do dispositivo), não pelo ID do dispositivo.
     * Isso permite detectar casos onde o mesmo dispositivo físico gera fingerprints diferentes
     * ou onde fingerprints são falsificados.
     * </p>
     * <p>
     * <strong>Lógica de Detecção:</strong>
     * <ol>
     *   <li>Extrai o fingerprint da transação atual</li>
     *   <li>Percorre o histórico de transações (excluindo a atual)</li>
     *   <li>Verifica se o fingerprint aparece em alguma transação anterior</li>
     *   <li>Retorna {@code true} se não houver ocorrências prévias</li>
     * </ol>
     * </p>
     *
     * @param transaction a transação atual sendo analisada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@code true} se o fingerprint nunca foi usado com este cartão antes,
     *         {@code false} se o fingerprint já existe no histórico ou se não estiver disponível
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
       VALIDAÇÕES INDIVIDUAIS
       ===================================================== */

    /**
     * Valida se a transação está sendo realizada em um dispositivo novo para o cartão.
     * <p>
     * Esta validação dispara quando um dispositivo nunca usado anteriormente com o cartão
     * é detectado, indicando possível primeiro uso em novo aparelho (legítimo) ou uso
     * fraudulento em dispositivo não autorizado.
     * </p>
     * <p>
     * <strong>Condições de Ativação:</strong>
     * <ul>
     *   <li>O cartão possui histórico de transações anteriores</li>
     *   <li>O ID do dispositivo atual não aparece em nenhuma transação prévia</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Condições de NÃO Ativação:</strong>
     * <ul>
     *   <li>Primeira transação do cartão (não há baseline para comparação)</li>
     *   <li>Dispositivo já foi usado anteriormente com o cartão</li>
     *   <li>Dados do dispositivo não disponíveis</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 15 pontos
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se não houver histórico ou se o dispositivo
     *         já for conhecido.
     * @see AlertType#NEW_DEVICE_DETECTED
     * @see #isNewDevice(Transaction, TransactionGlobalValidation.ValidationSnapshot)
     */
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

    /**
     * Valida se houve mudança no fingerprint de um dispositivo já conhecido.
     * <p>
     * Esta validação detecta quando o MESMO dispositivo (identificado por ID) apresenta
     * um fingerprint diferente do histórico. Isso pode indicar clonagem de ID de dispositivo,
     * manipulação de dados ou comprometimento do dispositivo.
     * </p>
     * <p>
     * <strong>Cenário de Fraude Típico:</strong>
     * <br>
     * Fraudador obtém o ID de um dispositivo legítimo mas não consegue replicar
     * o fingerprint completo, resultando em discrepância detectável.
     * </p>
     * <p>
     * <strong>Condições de Ativação:</strong>
     * <ul>
     *   <li>Dispositivo já existe no histórico (não é novo)</li>
     *   <li>Existe transação anterior do mesmo dispositivo</li>
     *   <li>Fingerprint atual difere do fingerprint histórico do dispositivo</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Condições de NÃO Ativação:</strong>
     * <ul>
     *   <li>Dispositivo é novo (tratado por {@link #newDeviceDetected})</li>
     *   <li>Fingerprint é idêntico ao histórico</li>
     *   <li>Não há transações anteriores do mesmo dispositivo</li>
     *   <li>Dados de dispositivo ou fingerprint não disponíveis</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 25 pontos
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @param snapshot    snapshot contendo histórico de transações do cartão
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se as condições de mudança não forem atendidas.
     * @see AlertType#DEVICE_FINGERPRINT_CHANGE
     * @see #isNewDevice(Transaction, TransactionGlobalValidation.ValidationSnapshot)
     */
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

    /**
     * Valida se a transação está sendo realizada através de Tor, proxy ou VPN.
     * <p>
     * Esta validação detecta o uso de redes anônimas ou proxies para ocultar
     * a origem real da transação, comportamento frequentemente associado a
     * atividades fraudulentas ou tentativas de contornar sistemas de segurança.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Extrai o endereço IP da transação</li>
     *   <li>Converte para objeto IPAddress para processamento</li>
     *   <li>Verifica se o IP está contido em algum CIDR da blacklist</li>
     *   <li>Sinaliza quando há correspondência</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 35 pontos
     * <br>
     * Sinal forte de tentativa de ocultação, embora possa haver casos legítimos
     * de uso de VPN para privacidade.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: o IP não estiver disponível, a blacklist
     *         não estiver carregada, ou o IP não estiver na blacklist.
     * @see AlertType#TOR_OR_PROXY_DETECTED
     * @see #loadVpnBlacklist()
     */
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

    /**
     * Valida se o mesmo dispositivo está sendo usado com múltiplos cartões diferentes.
     * <p>
     * Esta validação detecta padrões suspeitos onde um único dispositivo físico
     * é utilizado com vários cartões distintos, comportamento típico de testadores
     * de cartão (card testing), fraudadores organizados ou dispositivos comprometidos
     * usados para processar cartões roubados.
     * </p>
     * <p>
     * <strong>Estratégia de Detecção:</strong>
     * <ol>
     *   <li>Recupera a lista de cartões associados ao dispositivo</li>
     *   <li>Conta o número de cartões distintos</li>
     *   <li>Sinaliza se o número for igual ou superior ao threshold (3 cartões)</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Critério de Ativação:</strong>
     * <br>
     * {@code número_de_cartões >= 3}
     * </p>
     * <p>
     * <strong>Peso de Risco:</strong> 50 pontos
     * <br>
     * Sinal muito forte de atividade fraudulenta, especialmente quando combinado
     * com outros indicadores como velocidade de transações ou valores atípicos.
     * </p>
     *
     * @param transaction a transação atual sendo validada
     * @return {@link ValidationResultDto} contendo a pontuação e alertas identificados.
     *         Retorna resultado vazio se: o dispositivo não estiver disponível,
     *         a lista de cartões não estiver disponível, ou o número de cartões
     *         for inferior ao threshold.
     * @see AlertType#MULTIPLE_CARDS_SAME_DEVICE
     */
    public ValidationResultDto multipleCardsSameDevice(Transaction transaction) {
        ValidationResultDto result = new ValidationResultDto();

        Device device = transaction.getDevice();
        if (device == null || device.getCards() == null) return result;

        int distinctCards = device.getCards().size();
        if (distinctCards >= MULTIPLE_CARDS_THRESHOLD) {
            result.addScore(AlertType.MULTIPLE_CARDS_SAME_DEVICE.getScore());
            result.addAlert(AlertType.MULTIPLE_CARDS_SAME_DEVICE);
        }

        return result;
    }
}