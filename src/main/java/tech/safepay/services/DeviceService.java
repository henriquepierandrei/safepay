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
import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço responsável pela gestão completa de dispositivos e seus vínculos com cartões de crédito.
 * <p>
 * Este serviço oferece funcionalidades essenciais para o gerenciamento de dispositivos, incluindo:
 * <ul>
 *   <li>Geração e registro de dispositivos com fingerprints únicos</li>
 *   <li>Vinculação e desvinculação de cartões a dispositivos</li>
 *   <li>Atualização de fingerprints de dispositivos</li>
 *   <li>Consulta paginada com filtros avançados</li>
 *   <li>Distribuição automática de cartões entre dispositivos</li>
 * </ul>
 * <p>
 * O serviço implementa regras de segurança e limitações para prevenir uso indevido,
 * como limites de dispositivos suportados e validação de vínculos existentes.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@Transactional
public class DeviceService {

    /**
     * Número máximo de dispositivos que podem ser registrados simultaneamente no sistema.
     * Esta limitação previne sobrecarga e garante rastreabilidade adequada.
     */
    private static final int MAX_DEVICE_SUPPORTED = 20;

    /**
     * Gerador de números aleatórios thread-safe utilizado para seleção de configurações de dispositivos.
     */
    private static final Random RANDOM = new Random();

    /**
     * Opções de sistemas operacionais disponíveis para dispositivos Desktop.
     */
    private static final String[] OS_OPTIONS_DESKTOP = {"Windows 10", "Windows 11", "Linux", "macOS"};

    /**
     * Opções de sistemas operacionais disponíveis para dispositivos Mobile.
     */
    private static final String[] OS_OPTIONS_MOBILE = {"Android", "iOS"};

    /**
     * Navegadores web disponíveis para dispositivos Desktop.
     */
    private static final String[] DESKTOP_BROWSERS = {"Chrome", "Firefox", "Edge", "Safari"};

    /**
     * Navegadores web disponíveis para dispositivos Mobile.
     */
    private static final String[] MOBILE_BROWSERS  = {"Chrome Mobile", "Safari Mobile", "Samsung Internet"};

    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param cardRepository repositório para operações de persistência de cartões
     * @param deviceRepository repositório para operações de persistência de dispositivos
     */
    public DeviceService(CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Seleciona aleatoriamente dois cartões para associação com um dispositivo.
     * <p>
     * Este método é utilizado durante a criação de dispositivos ou distribuição automática
     * de cartões, garantindo que cada dispositivo tenha pelo menos dois cartões associados
     * para simular comportamento realista de usuários.
     * <p>
     * A seleção é feita de forma aleatória para distribuir uniformemente os cartões
     * entre os dispositivos do sistema.
     *
     * @return lista contendo exatamente dois cartões selecionados aleatoriamente
     * @throws IllegalStateException se houver menos de 2 cartões disponíveis no sistema
     */
    public List<Card> sortCardToDevice() {
        List<Card> cards = new ArrayList<>(cardRepository.findAll());
        if (cards.size() < 2) {
            throw new IllegalStateException("Not enough cards to associate with device");
        }
        Collections.shuffle(cards);
        return cards.subList(0, 2);
    }

    /**
     * Gera e registra um lote de novos dispositivos no sistema.
     * <p>
     * Cada dispositivo é criado com as seguintes características:
     * <ul>
     *   <li><b>Fingerprint único:</b> identificador UUID exclusivo para rastreamento</li>
     *   <li><b>Cartões vinculados:</b> dois cartões aleatórios associados inicialmente</li>
     *   <li><b>Tipo de dispositivo:</b> Desktop, Mobile ou outro tipo selecionado aleatoriamente</li>
     *   <li><b>Sistema operacional:</b> compatível com o tipo de dispositivo</li>
     *   <li><b>Navegador:</b> compatível com o tipo de dispositivo</li>
     *   <li><b>Timestamps:</b> registros de primeira e última visualização</li>
     * </ul>
     * <p>
     * <b>Regras de negócio:</b>
     * <ul>
     *   <li>O número total de dispositivos não pode exceder {@value #MAX_DEVICE_SUPPORTED}</li>
     *   <li>Cada dispositivo é criado com timestamps idênticos de first/last seen</li>
     *   <li>Dispositivos são salvos em lote para otimização de performance</li>
     * </ul>
     *
     * @param quantity número de dispositivos a serem criados
     * @return DeviceResponse com mensagem de sucesso e status HTTP OK
     * @throws DeviceMaxSupportedException se a quantidade solicitada exceder o limite permitido
     * @throws IllegalStateException se não houver cartões suficientes para associação
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

    /**
     * Seleciona aleatoriamente um sistema operacional compatível com o tipo de dispositivo.
     * <p>
     * A seleção é feita com base no tipo do dispositivo:
     * <ul>
     *   <li><b>DESKTOP:</b> Windows 10, Windows 11, Linux ou macOS</li>
     *   <li><b>MOBILE:</b> Android ou iOS</li>
     *   <li><b>Outros tipos:</b> Embedded Linux (padrão)</li>
     * </ul>
     *
     * @param type tipo do dispositivo (Desktop, Mobile, etc.)
     * @return string representando o sistema operacional selecionado
     */
    private String randomOs(DeviceType type) {
        return switch (type) {
            case DESKTOP -> OS_OPTIONS_DESKTOP[RANDOM.nextInt(OS_OPTIONS_DESKTOP.length)];
            case MOBILE -> OS_OPTIONS_MOBILE[RANDOM.nextInt(OS_OPTIONS_MOBILE.length)];
            default -> "Embedded Linux";
        };
    }

    /**
     * Seleciona aleatoriamente um navegador web compatível com o tipo de dispositivo.
     * <p>
     * A seleção é feita com base no tipo do dispositivo:
     * <ul>
     *   <li><b>DESKTOP:</b> Chrome, Firefox, Edge ou Safari</li>
     *   <li><b>MOBILE:</b> Chrome Mobile, Safari Mobile ou Samsung Internet</li>
     *   <li><b>Outros tipos:</b> N/A (não aplicável)</li>
     * </ul>
     *
     * @param type tipo do dispositivo (Desktop, Mobile, etc.)
     * @return string representando o navegador selecionado
     */
    private String randomBrowser(DeviceType type) {
        return switch (type) {
            case DESKTOP -> DESKTOP_BROWSERS[RANDOM.nextInt(DESKTOP_BROWSERS.length)];
            case MOBILE -> MOBILE_BROWSERS[RANDOM.nextInt(MOBILE_BROWSERS.length)];
            default -> "N/A";
        };
    }

    /**
     * Adiciona manualmente um cartão específico a um dispositivo específico.
     * <p>
     * Este método estabelece um vínculo bidirecional entre o cartão e o dispositivo,
     * garantindo a consistência do relacionamento Many-to-Many em ambas as direções.
     * <p>
     * <b>Características:</b>
     * <ul>
     *   <li>Previne duplicatas: verifica se o vínculo já existe antes de adicionar</li>
     *   <li>Relacionamento bidirecional: atualiza ambas as entidades</li>
     *   <li>Persistência gerenciada pelo JPA: salva apenas um lado da relação</li>
     * </ul>
     *
     * @param dto objeto contendo cardId e deviceId para estabelecer o vínculo
     * @return DeviceResponse com mensagem de confirmação e status HTTP OK
     * @throws CardNotFoundException se o cartão com o ID especificado não existir
     * @throws DeviceNotFoundException se o dispositivo com o ID especificado não existir
     */
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

    /**
     * Distribui automaticamente cartões aleatórios para todos os dispositivos existentes.
     * <p>
     * Este método é útil para:
     * <ul>
     *   <li>Configuração inicial do sistema de testes</li>
     *   <li>População rápida de dados de demonstração</li>
     *   <li>Simulação de distribuição uniforme de cartões</li>
     * </ul>
     * <p>
     * <b>Comportamento:</b>
     * <ol>
     *   <li>Seleciona dois cartões aleatórios do sistema</li>
     *   <li>Adiciona esses cartões a TODOS os dispositivos existentes</li>
     *   <li>Evita duplicatas verificando vínculos existentes</li>
     *   <li>Salva todas as alterações em lote</li>
     * </ol>
     * <p>
     * <b>Atenção:</b> Esta operação afeta TODOS os dispositivos do sistema.
     *
     * @return DeviceResponse com mensagem informando o resultado da operação
     * @throws IllegalStateException se não houver cartões suficientes no sistema
     */
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

    /**
     * Recupera uma lista paginada de dispositivos com filtros opcionais aplicados.
     * <p>
     * Este método suporta os seguintes filtros:
     * <ul>
     *   <li><b>deviceType:</b> filtra por tipo de dispositivo (Desktop, Mobile, etc.)</li>
     *   <li><b>os:</b> filtra por sistema operacional específico</li>
     *   <li><b>browser:</b> filtra por navegador específico</li>
     * </ul>
     * <p>
     * Os resultados são retornados em formato paginado para otimizar performance
     * e experiência do usuário ao lidar com grandes volumes de dispositivos.
     * <p>
     * <b>Paginação:</b>
     * <ul>
     *   <li>A primeira página tem índice 0</li>
     *   <li>O tamanho da página controla quantos registros são retornados</li>
     *   <li>A resposta inclui metadados de paginação (total de páginas, total de elementos, etc.)</li>
     * </ul>
     *
     * @param deviceType tipo do dispositivo para filtrar (opcional, pode ser null)
     * @param os sistema operacional para filtrar (opcional, pode ser null)
     * @param browser navegador para filtrar (opcional, pode ser null)
     * @param page número da página a ser recuperada (zero-based)
     * @param size quantidade de registros por página
     * @return Page&lt;DeviceListResponseDto.DeviceDto&gt; contendo os dispositivos que atendem aos critérios
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


    /**
     * Remove o vínculo entre um cartão e um dispositivo específicos.
     * <p>
     * Esta operação desfaz a associação entre as entidades sem excluir nenhum registro.
     * O cartão e o dispositivo continuam existindo no sistema, apenas não estarão mais vinculados.
     * <p>
     * <b>Validações executadas:</b>
     * <ul>
     *   <li>Verifica se o cartão existe no sistema</li>
     *   <li>Verifica se o dispositivo existe no sistema</li>
     *   <li>Confirma que existe um vínculo ativo entre eles</li>
     * </ul>
     * <p>
     * <b>Importante:</b> Esta operação só remove o vínculo do lado do cartão.
     * O JPA cuida da sincronização do relacionamento bidirecional.
     *
     * @param cardId identificador único do cartão
     * @param deviceId identificador único do dispositivo
     * @return DeviceResponse com mensagem de confirmação e status HTTP OK
     * @throws CardNotFoundException se o cartão com o ID especificado não existir
     * @throws DeviceNotFoundException se o dispositivo com o ID especificado não existir
     * @throws DeviceNotLinkedException se não existir vínculo ativo entre o cartão e o dispositivo
     */
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

    /**
     * Atualiza o fingerprint de um dispositivo específico.
     * <p>
     * Esta operação gera um novo UUID único para o dispositivo e registra o timestamp
     * da alteração. É utilizada em cenários como:
     * <ul>
     *   <li>Suspeita de comprometimento do dispositivo</li>
     *   <li>Solicitação de renovação de credenciais</li>
     *   <li>Testes de segurança e auditoria</li>
     *   <li>Reconfiguração após manutenção</li>
     * </ul>
     * <p>
     * <b>Importante:</b> A alteração de fingerprint pode impactar:
     * <ul>
     *   <li>Histórico de análise de fraude baseado em dispositivo</li>
     *   <li>Autenticação contínua e sessões ativas</li>
     *   <li>Rastreabilidade de transações</li>
     * </ul>
     * <p>
     * O timestamp da última alteração é registrado para fins de auditoria e segurança.
     *
     * @param deviceId identificador único do dispositivo
     * @return DeviceResponse com mensagem de confirmação e status HTTP OK
     * @throws DeviceNotFoundException se o dispositivo com o ID especificado não existir
     */
    public DeviceResponse updateFingerPrint(UUID deviceId) {
        var optionalDevice = deviceRepository.findById(deviceId).orElseThrow(() -> new DeviceNotFoundException("Não foi possível encontrar o dispositivo."));
        UUID newFingerPrint = UUID.randomUUID();

        optionalDevice.setFingerPrintId(newFingerPrint.toString());
        optionalDevice.setLastFingerPrintChangedAt(LocalDateTime.now());
        deviceRepository.save(optionalDevice);
        return new DeviceResponse(
                "Biometria atualizada com sucesso!",
                HttpStatus.OK
        );

    }

    /**
     * Objeto de resposta padrão para operações de dispositivo.
     * <p>
     * Encapsula a mensagem de retorno e o status HTTP da operação,
     * proporcionando consistência nas respostas da API.
     *
     * @param message mensagem descritiva do resultado da operação
     * @param status código de status HTTP indicando sucesso ou tipo de erro
     */
    public record DeviceResponse(String message, HttpStatus status) {
    }

    /**
     * DTO para requisições de adição de cartão a dispositivo.
     * <p>
     * Encapsula os identificadores necessários para estabelecer o vínculo
     * entre um cartão e um dispositivo específicos.
     *
     * @param cardId identificador único do cartão a ser vinculado
     * @param deviceId identificador único do dispositivo que receberá o cartão
     */
    public record AddCardDto(UUID cardId, UUID deviceId) {
    }


}