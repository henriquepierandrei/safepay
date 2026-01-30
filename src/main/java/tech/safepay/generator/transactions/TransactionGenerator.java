package tech.safepay.generator.transactions;

import org.springframework.stereotype.Component;
import tech.safepay.Enums.CardStatus;
import tech.safepay.Enums.TransactionDecision;
import tech.safepay.configs.ResolveLocalizationConfig;
import tech.safepay.dtos.transaction.ManualTransactionDto;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.exceptions.card.CardBlockedOrLostException;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.exceptions.device.DeviceNotLinkedException;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Gerador central de transações simuladas para testes de sistema antifraude.
 * <p>
 * Este componente orquestra a criação de transações realistas agregando múltiplos geradores
 * especializados para produzir eventos transacionais completos e consistentes, incluindo:
 * <ul>
 *   <li>Seleção inteligente de cartões e dispositivos</li>
 *   <li>Geração de valores baseada em histórico comportamental</li>
 *   <li>Categorias de comerciantes consistentes com perfil do usuário</li>
 *   <li>Endereços IP realistas (incluindo VPNs)</li>
 *   <li>Coordenadas GPS baseadas em cidades reais</li>
 *   <li>Resolução de localização (país, estado, cidade)</li>
 * </ul>
 * <p>
 * <b>Modos de operação:</b>
 * <ul>
 *   <li><b>Automático (generateNormalTransaction):</b> transação completamente aleatória
 *       seguindo padrões comportamentais realistas</li>
 *   <li><b>Manual (generateManualTransaction):</b> transação determinística com dados
 *       explicitamente fornecidos via DTO</li>
 * </ul>
 * <p>
 * <b>Importante - Responsabilidades:</b>
 * Este gerador é responsável APENAS pela criação do evento transacional.
 * Ele NÃO:
 * <ul>
 *   <li>Executa validações antifraude</li>
 *   <li>Calcula scores de risco</li>
 *   <li>Define decisão final (APPROVED/BLOCKED)</li>
 *   <li>Altera limites de crédito</li>
 *   <li>Cria alertas de fraude</li>
 * </ul>
 * <p>
 * Todas as transações são criadas em estado neutro (REVIEW) e posteriormente
 * processadas pelo pipeline antifraude (TransactionDecisionService).
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Geração de datasets para testes automatizados</li>
 *   <li>Simulação de volume transacional para load testing</li>
 *   <li>Criação de cenários específicos de fraude</li>
 *   <li>Treinamento de modelos de machine learning</li>
 *   <li>Reprodução de incidentes para debugging</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class TransactionGenerator {

    /**
     * Gerador de números aleatórios thread-safe para seleção de cartões e dispositivos.
     */
    private static final Random RANDOM = new Random();

    private final MerchantCategoryGenerator merchantCategoryGenerator;
    private final AmountGenerator amountGenerator;
    private final IPGenerator ipGenerator;
    private final LatitudeAndLongitudeGenerator generateLocation;

    private final ResolveLocalizationConfig resolveLocalizationConfig;

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Construtor do gerador com injeção de todas as dependências necessárias.
     *
     * @param merchantCategoryGenerator gerador de categorias de comerciantes baseado em histórico
     * @param amountGenerator gerador de valores transacionais inteligentes
     * @param ipGenerator gerador de endereços IPv6 (normais e VPN)
     * @param generateLocation gerador de coordenadas GPS baseadas em cidades reais
     * @param cardRepository repositório para consulta e persistência de cartões
     * @param transactionRepository repositório para persistência de transações
     * @param deviceRepository repositório para consulta de dispositivos
     * @param resolveLocalizationConfig serviço de geocoding reverso para resolução de endereços
     */
    public TransactionGenerator(
            MerchantCategoryGenerator merchantCategoryGenerator,
            AmountGenerator amountGenerator,
            IPGenerator ipGenerator,
            LatitudeAndLongitudeGenerator generateLocation,
            CardRepository cardRepository,
            TransactionRepository transactionRepository,
            DeviceRepository deviceRepository,
            ResolveLocalizationConfig resolveLocalizationConfig
    ) {
        this.merchantCategoryGenerator = merchantCategoryGenerator;
        this.amountGenerator = amountGenerator;
        this.ipGenerator = ipGenerator;
        this.generateLocation = generateLocation;
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.deviceRepository = deviceRepository;
        this.resolveLocalizationConfig = resolveLocalizationConfig;
    }

    /**
     * Seleciona aleatoriamente um cartão disponível que possua dispositivos vinculados.
     * <p>
     * Este método garante que apenas cartões válidos e ativos no sistema sejam utilizados
     * para geração de transações, filtrando:
     * <ul>
     *   <li>Cartões que possuem pelo menos um dispositivo associado</li>
     *   <li>Cartões em estado utilizável pelo sistema</li>
     * </ul>
     * <p>
     * <b>Critérios de seleção:</b>
     * <ul>
     *   <li>Cartão deve ter devices.size() > 0</li>
     *   <li>Seleção uniforme entre todos os cartões elegíveis</li>
     * </ul>
     * <p>
     * <b>Por que exigir dispositivos?</b>
     * Transações realistas sempre ocorrem através de um dispositivo específico
     * (terminal POS, smartphone, computador). Cartões sem dispositivos não podem
     * gerar transações válidas no fluxo normal do sistema.
     *
     * @return cartão selecionado aleatoriamente entre os elegíveis
     * @throws IllegalStateException se não houver cartões com dispositivos no sistema
     */
    private Card sortCard() {
        var cards = cardRepository.findByDevicesIsNotEmpty();

        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards available to generate transactions");
        }

        return cards.get(RANDOM.nextInt(cards.size()));
    }

    /**
     * Gera uma transação automática simulando comportamento legítimo de usuário.
     * <p>
     * Este método cria uma transação completa de forma totalmente automática, utilizando
     * geradores especializados para cada componente da transação. Todos os dados são
     * gerados respeitando padrões comportamentais realistas e histórico do cartão.
     * <p>
     * <b>Processo de geração:</b>
     * <ol>
     *   <li><b>Seleção de cartão:</b> escolhe cartão aleatório com dispositivos</li>
     *   <li><b>Categoria de comerciante:</b> baseada em histórico (90% padrão, 10% anomalia)</li>
     *   <li><b>Valor da transação:</b> consistente com média histórica ou valor alto anômalo</li>
     *   <li><b>Endereço IP:</b> IPv6 normal (95%) ou de VPN conhecida (5%)</li>
     *   <li><b>Dispositivo:</b> seleciona um dos dispositivos vinculados ao cartão</li>
     *   <li><b>Localização GPS:</b> próxima à última transação (95%) ou cidade diferente (5%)</li>
     *   <li><b>Geocoding reverso:</b> resolve coordenadas em país, estado e cidade</li>
     *   <li><b>Timestamps:</b> data/hora atual do sistema</li>
     *   <li><b>Estado inicial:</b> REVIEW (aguardando processamento antifraude)</li>
     * </ol>
     * <p>
     * <b>Características importantes:</b>
     * <ul>
     *   <li><b>Não determinístico:</b> cada execução produz transação diferente</li>
     *   <li><b>Comportamento realista:</b> segue padrões estatísticos calibrados</li>
     *   <li><b>Consistência interna:</b> dados são coerentes entre si (cartão-dispositivo-localização)</li>
     *   <li><b>Estado neutro:</b> sem pré-julgamento (decisão será feita pelo antifraude)</li>
     * </ul>
     * <p>
     * <b>O que este método NÃO faz:</b>
     * <ul>
     *   <li>Não executa validações antifraude</li>
     *   <li>Não calcula score de risco</li>
     *   <li>Não define decisão final (sempre REVIEW inicialmente)</li>
     *   <li>Não altera limite de crédito do cartão</li>
     *   <li>Não cria alertas de fraude</li>
     *   <li>Não valida se cartão está expirado ou bloqueado</li>
     * </ul>
     * <p>
     * <b>Fluxo posterior:</b>
     * Após criação, a transação deve ser processada por:
     * <ol>
     *   <li>TransactionDecisionService (avaliação antifraude)</li>
     *   <li>FraudAlertFactory (criação de alertas se necessário)</li>
     *   <li>CardPatternService (atualização de padrões comportamentais)</li>
     * </ol>
     * <p>
     * <b>Exemplo de transação gerada:</b>
     * <pre>
     * {
     *   "card": { "id": "uuid-123", "number": "**** 3456" },
     *   "amount": 127.50,
     *   "merchantCategory": "GROCERY",
     *   "ipAddress": "2001:db8:85a3::8a2e:370:7334",
     *   "device": { "id": "uuid-456", "fingerprint": "abc123..." },
     *   "latitude": "-23.548520",
     *   "longitude": "-46.638308",
     *   "city": "São Paulo",
     *   "state": "SP",
     *   "countryCode": "BR",
     *   "transactionDecision": "REVIEW",
     *   "fraud": false
     * }
     * </pre>
     *
     * @return transação gerada e persistida no banco de dados em estado REVIEW
     * @throws IllegalStateException se não houver cartões disponíveis ou se cartão não tiver dispositivos
     */
    public Transaction generateNormalTransaction() {

        Card card = sortCard();

        if (card.getStatus().equals(CardStatus.BLOCKED) || card.getStatus().equals(CardStatus.LOST)) {
            throw new CardBlockedOrLostException("Cartão está bloqueado ou perdido.");
        }

        Transaction transaction = new Transaction();

        transaction.setCard(card);

        // Categoria do merchant baseada no perfil do cartão
        transaction.setMerchantCategory(
                merchantCategoryGenerator.sortMerchant(card)
        );

        // Valor da transação (respeita limite, status e expiração)
        transaction.setAmount(
                amountGenerator.generateAmount(card)
        );

        transaction.setTransactionDateAndTime(LocalDateTime.now());
        transaction.setReimbursement(false);


        transaction.setCreatedAt(LocalDateTime.now());

        // IP
        transaction.setIpAddress(
                ipGenerator.generateIP()
        );

        // Device
        var devices = card.getDevices();
        if (devices == null || devices.isEmpty()) {
            throw new IllegalStateException(
                    "Card " + card.getCardId() + " has no associated devices"
            );
        }

        var device = devices.get(RANDOM.nextInt(devices.size()));

        transaction.setDevice(device);
        transaction.setDeviceFingerprint(device.getFingerPrintId());

        // Localização baseada no histórico do cartão
        String[] location = generateLocation.generateLocation(card);
        transaction.setLatitude(location[0]);
        transaction.setLongitude(location[1]);


        // Localizacao exata

        ResolvedLocalizationDto resolvedLocalizationDto = resolveLocalizationConfig.resolve(location[0], location[1]);
        transaction.setCountryCode(resolvedLocalizationDto.countryCode());
        transaction.setState(resolvedLocalizationDto.state());
        transaction.setCity(resolvedLocalizationDto.city());


        // Persistência inicial (estado neutro)
        transaction.setTransactionDecision(TransactionDecision.REVIEW);
        transaction.setFraud(false);

        return transactionRepository.save(transaction);
    }




    /**
     * Gera uma transação de forma manual e determinística com dados explicitamente fornecidos.
     * <p>
     * Este método permite criação controlada de transações onde todos os parâmetros são
     * especificados previamente, oferecendo controle total sobre o cenário transacional.
     * É o oposto de {@link #generateNormalTransaction()}: nada é aleatório, tudo é determinístico.
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li><b>Testes funcionais:</b> criar cenários específicos reproduzíveis</li>
     *   <li><b>Simulação de fraude:</b> gerar transações com características suspeitas conhecidas</li>
     *   <li><b>Reprodução de incidentes:</b> recriar transações problemáticas para debugging</li>
     *   <li><b>Operações administrativas:</b> inserção manual de transações para correção de dados</li>
     *   <li><b>Testes de edge cases:</b> valores extremos, localizações específicas, etc.</li>
     * </ul>
     * <p>
     * <b>Processo de criação:</b>
     * <ol>
     *   <li><b>Validação de cartão:</b> verifica existência no banco de dados</li>
     *   <li><b>Validação de dispositivo:</b> verifica existência no banco de dados</li>
     *   <li><b>Validação de vínculo:</b> confirma que dispositivo pertence ao cartão</li>
     *   <li><b>Atribuição de dados:</b> utiliza exatamente os valores do DTO</li>
     *   <li><b>Geocoding reverso:</b> resolve coordenadas em localização textual</li>
     *   <li><b>Timestamps:</b> data/hora atual (não fornecida no DTO)</li>
     *   <li><b>Estado inicial:</b> REVIEW, ou APPROVED se successForce = true</li>
     * </ol>
     * <p>
     * <b>Validações executadas:</b>
     * <table border="1">
     *   <tr>
     *     <th>Validação</th>
     *     <th>Exceção</th>
     *     <th>Motivo</th>
     *   </tr>
     *   <tr>
     *     <td>Cartão existe</td>
     *     <td>IllegalArgumentException</td>
     *     <td>cardId inválido ou não cadastrado</td>
     *   </tr>
     *   <tr>
     *     <td>Dispositivo existe</td>
     *     <td>IllegalArgumentException</td>
     *     <td>deviceId inválido ou não cadastrado</td>
     *   </tr>
     *   <tr>
     *     <td>Dispositivo vinculado ao cartão</td>
     *     <td>IllegalStateException</td>
     *     <td>Inconsistência de dados (device não pertence ao card)</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Parâmetro successForce:</b>
     * <ul>
     *   <li><b>false:</b> transação criada em estado REVIEW (processamento normal)</li>
     *   <li><b>true:</b> transação criada em estado APPROVED (bypass de antifraude para testes)</li>
     * </ul>
     * <p>
     * <b>O que este método NÃO faz:</b>
     * <ul>
     *   <li>Não executa validações antifraude (exceto se successForce = false posteriormente)</li>
     *   <li>Não calcula score de risco</li>
     *   <li>Não valida consistência dos dados fornecidos (IP vs GPS, valor vs limite, etc.)</li>
     *   <li>Não altera limite de crédito do cartão</li>
     *   <li>Não cria alertas de fraude</li>
     *   <li>Não valida se cartão está expirado ou bloqueado</li>
     * </ul>
     * <p>
     * <b>Diferenças vs generateNormalTransaction:</b>
     * <table border="1">
     *   <tr>
     *     <th>Aspecto</th>
     *     <th>generateNormalTransaction</th>
     *     <th>generateManualTransaction</th>
     *   </tr>
     *   <tr>
     *     <td>Cartão</td>
     *     <td>Aleatório</td>
     *     <td>Especificado no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>Valor</td>
     *     <td>Gerado por histórico</td>
     *     <td>Especificado no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>Categoria</td>
     *     <td>Gerada por padrão</td>
     *     <td>Especificada no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>IP</td>
     *     <td>Gerado (95% normal, 5% VPN)</td>
     *     <td>Especificado no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>GPS</td>
     *     <td>Gerado por histórico</td>
     *     <td>Especificado no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>Dispositivo</td>
     *     <td>Aleatório do cartão</td>
     *     <td>Especificado no DTO</td>
     *   </tr>
     *   <tr>
     *     <td>Decisão inicial</td>
     *     <td>Sempre REVIEW</td>
     *     <td>REVIEW ou APPROVED (successForce)</td>
     *   </tr>
     *   <tr>
     *     <td>Reprodutibilidade</td>
     *     <td>Não (aleatório)</td>
     *     <td>Sim (determinístico)</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Exemplo de uso:</b>
     * <pre>
     * // Criar transação suspeita para teste
     * ManualTransactionDto dto = new ManualTransactionDto(
     *     cardId,
     *     deviceId,
     *     BigDecimal.valueOf(5000.00),  // valor muito alto
     *     MerchantCategory.GAMBLING,     // categoria de risco
     *     "2001:67c:2e8::1234",          // IP de VPN
     *     "-34.603684",                  // Buenos Aires
     *     "-58.381559"
     * );
     *
     * Transaction tx = generator.generateManualTransaction(dto, false);
     * // Transação criada em REVIEW, será processada pelo antifraude
     * </pre>
     * <p>
     * <b>Fluxo posterior:</b>
     * Se successForce = false, a transação segue para processamento normal:
     * <ol>
     *   <li>TransactionDecisionService (avaliação antifraude)</li>
     *   <li>FraudAlertFactory (alertas se necessário)</li>
     *   <li>CardPatternService (atualização de padrões)</li>
     * </ol>
     * <p>
     * Se successForce = true, a transação é considerada aprovada imediatamente,
     * útil para testes de fluxo de transações aprovadas sem passar pelo antifraude.
     *
     * @param manualTransactionDto DTO contendo todos os dados necessários para criação explícita
     *                             da transação (cardId, deviceId, amount, category, IP, GPS)
     * @param successForce se true, força decisão APPROVED; se false, inicia em REVIEW
     * @return transação persistida no banco de dados com dados exatamente conforme especificado
     * @throws IllegalArgumentException se cartão ou dispositivo não existirem no banco
     * @throws IllegalStateException se dispositivo não estiver vinculado ao cartão especificado
     */
    public Transaction generateManualTransaction(ManualTransactionDto manualTransactionDto, boolean successForce) {

        // Recupera o cartão informado
        Card card = cardRepository.findById(manualTransactionDto.cardId())
                .orElseThrow(() ->
                        new CardNotFoundException("Cartão encontrado!")
                );

        if (card.getStatus().equals(CardStatus.BLOCKED) || card.getStatus().equals(CardStatus.LOST)) {
            throw new CardBlockedOrLostException("Cartão está bloqueado ou perdido.");
        }

        // Recupera o device informado
        var device = deviceRepository.findById(manualTransactionDto.deviceId())
                .orElseThrow(() ->
                        new DeviceNotFoundException("Dispositivo não encontrado.")
                );

        // Garante que o device pertence ao cartão informado
        if (card.getDevices() == null || !card.getDevices().contains(device)) {
            throw new DeviceNotLinkedException(
                    "Dispositivo não possui esse cartão"
            );
        }

        Transaction transaction = new Transaction();
        transaction.setCard(card);
        transaction.setDevice(device);
        transaction.setDeviceFingerprint(device.getFingerPrintId());

        transaction.setAmount(manualTransactionDto.amount());
        transaction.setMerchantCategory(manualTransactionDto.merchantCategory());

        transaction.setIpAddress(manualTransactionDto.ipAddress());
        transaction.setLatitude(manualTransactionDto.latitude());
        transaction.setLongitude(manualTransactionDto.longitude());

        ResolvedLocalizationDto resolvedLocalizationDto = resolveLocalizationConfig.resolve(manualTransactionDto.latitude(), manualTransactionDto.longitude());
        transaction.setCountryCode(resolvedLocalizationDto.countryCode());
        transaction.setState(resolvedLocalizationDto.state());
        transaction.setCity(resolvedLocalizationDto.city());


        transaction.setTransactionDateAndTime(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setReimbursement(false);

        // Estado inicial da transação
        transaction.setTransactionDecision(TransactionDecision.REVIEW);
        if (successForce){
            transaction.setTransactionDecision(TransactionDecision.APPROVED);
        }
        transaction.setFraud(false);

        return transactionRepository.save(transaction);
    }


}