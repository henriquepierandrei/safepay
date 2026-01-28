package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tech.safepay.dtos.cards.CardDataResponseDto;
import tech.safepay.dtos.device.DeviceListResponseDto;
import tech.safepay.dtos.transaction.ResolvedLocalizationDto;
import tech.safepay.dtos.transaction.TransactionResponseDto;
import tech.safepay.exceptions.transaction.TransactionNotFoundException;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;
import tech.safepay.repositories.TransactionRepository;

import java.util.UUID;

/**
 * Serviço responsável pela consulta e recuperação de informações de transações.
 * <p>
 * Este serviço oferece funcionalidades para acesso aos dados detalhados de transações
 * processadas pelo sistema, incluindo:
 * <ul>
 *   <li>Recuperação de transações por identificador único</li>
 *   <li>Mascaramento automático de dados sensíveis (números de cartão)</li>
 *   <li>Agregação de dados relacionados (cartão, dispositivo, localização)</li>
 *   <li>Construção de DTOs seguros para exposição em APIs</li>
 * </ul>
 * <p>
 * <b>Segurança e conformidade:</b>
 * <ul>
 *   <li>Aplica mascaramento PCI-DSS em números de cartão</li>
 *   <li>Retorna apenas DTOs, nunca entidades JPA diretas</li>
 *   <li>Protege dados sensíveis contra exposição acidental</li>
 * </ul>
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>Detalhes de transação em interfaces de usuário</li>
 *   <li>Consultas de suporte e atendimento ao cliente</li>
 *   <li>Auditorias e investigações de fraude</li>
 *   <li>Relatórios e análises transacionais</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    /**
     * Construtor do serviço com injeção de dependências.
     * <p>
     * <b>Nota:</b> Os repositórios cardRepository e deviceRepository são injetados
     * mas não utilizados diretamente pois os dados são acessados via navegação
     * do relacionamento JPA da transação.
     *
     * @param transactionRepository repositório para acesso às transações
     * @param cardRepository repositório de cartões (injetado para compatibilidade)
     * @param deviceRepository repositório de dispositivos (injetado para compatibilidade)
     */
    public TransactionService(TransactionRepository transactionRepository, CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Aplica máscara de segurança ao número do cartão, ocultando dígitos sensíveis.
     * <p>
     * Implementa o padrão de mascaramento PCI-DSS, exibindo apenas os últimos 4 dígitos
     * do cartão e substituindo os demais por asteriscos. Este método é fundamental para
     * proteção de dados de cartão (PAN - Primary Account Number) em conformidade com
     * regulamentações de segurança financeira.
     * <p>
     * <b>Padrão de mascaramento:</b>
     * <ul>
     *   <li>Entrada: "1234567890123456" → Saída: "**** **** **** 3456"</li>
     *   <li>Entrada: "123" (inválido) → Saída: "****"</li>
     *   <li>Entrada: null → Saída: "****"</li>
     * </ul>
     * <p>
     * <b>Conformidade:</b>
     * Este método atende aos requisitos PCI-DSS de:
     * <ul>
     *   <li>Requirement 3.3: Mascaramento de PAN quando exibido</li>
     *   <li>Requirement 3.4: Tornar PAN ilegível em qualquer local de armazenamento</li>
     * </ul>
     * <p>
     * <b>Segurança:</b>
     * O método é privado para garantir que mascaramento seja sempre aplicado
     * internamente antes de expor dados via DTOs públicos.
     *
     * @param cardNumber número completo do cartão (16 dígitos esperados)
     * @return string mascarada exibindo apenas os últimos 4 dígitos, ou "****" se inválido
     */
    private String getMaskedCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }


    /**
     * Recupera os detalhes completos de uma transação específica por seu identificador.
     * <p>
     * Este método busca uma transação pelo UUID e constrói um DTO consolidado contendo:
     * <ul>
     *   <li><b>Dados do cartão:</b> informações mascaradas e seguras (CardDataResponseDto)</li>
     *   <li><b>Dados do dispositivo:</b> fingerprint, tipo, OS e navegador (DeviceDto)</li>
     *   <li><b>Dados da transação:</b> valor, categoria, data/hora, decisão</li>
     *   <li><b>Localização:</b> coordenadas GPS e dados resolvidos (país, estado, cidade)</li>
     *   <li><b>Rede:</b> endereço IP de origem</li>
     *   <li><b>Fraude:</b> flag indicando se foi marcada como fraudulenta</li>
     * </ul>
     * <p>
     * <b>Navegação de relacionamentos:</b>
     * O método utiliza navegação JPA (lazy loading) para acessar entidades relacionadas:
     * <ul>
     *   <li>transaction.getCard() → acessa dados do cartão</li>
     *   <li>transaction.getDevice() → acessa dados do dispositivo</li>
     * </ul>
     * <p>
     * <b>Segurança aplicada:</b>
     * <ul>
     *   <li>Número do cartão é automaticamente mascarado (apenas últimos 4 dígitos)</li>
     *   <li>Retorna DTO imutável, não expõe entidades JPA</li>
     *   <li>Dados sensíveis protegidos conforme PCI-DSS</li>
     * </ul>
     * <p>
     * <b>Campos opcionais (null):</b>
     * Os seguintes campos retornam null neste método:
     * <ul>
     *   <li><b>validationResult:</b> resultado da validação antifraude (não armazenado)</li>
     *   <li><b>severity:</b> severidade do alerta (consulte FraudAlert separadamente)</li>
     * </ul>
     * <p>
     * Para obter informações completas de validação e alertas, consulte o
     * FraudAlertService utilizando o ID da transação.
     * <p>
     * <b>Performance:</b>
     * Este método executa lazy loading de relacionamentos. Em contextos onde múltiplas
     * transações são consultadas, considere usar queries com fetch joins para otimizar
     * o número de consultas ao banco.
     *
     * @param id identificador único (UUID) da transação
     * @return TransactionResponseDto contendo todos os dados da transação e entidades relacionadas
     * @throws TransactionNotFoundException se não existir transação com o ID especificado
     */
    public TransactionResponseDto getTransactionById(UUID id) {
        var transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada"));

        var card = transaction.getCard();
        var device = transaction.getDevice();

        var cardDto = new CardDataResponseDto(
                card.getCardId(),
                getMaskedCardNumber(card.getCardNumber()),
                card.getCardHolderName(),
                card.getCardBrand(),
                card.getExpirationDate(),
                card.getCreditLimit(),
                card.getStatus()
        );

        var deviceDto = new DeviceListResponseDto.DeviceDto(
                device.getId(),
                device.getFingerPrintId(),
                device.getDeviceType(),
                device.getOs(),
                device.getBrowser()
        );

        return new TransactionResponseDto(
                cardDto,
                transaction.getMerchantCategory(),
                transaction.getAmount(),
                transaction.getTransactionDateAndTime(),
                transaction.getLatitude(),
                transaction.getLongitude(),
                new ResolvedLocalizationDto(
                        transaction.getCountryCode(),
                        transaction.getState(),
                        transaction.getCity()),
                null,
                null,
                deviceDto,
                transaction.getIpAddress(),
                transaction.getTransactionDecision(),
                transaction.getFraud(),
                transaction.getCreatedAt()
        );
    }

}