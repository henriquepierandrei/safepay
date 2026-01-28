package tech.safepay.services;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.CardBrand;
import tech.safepay.Enums.CardStatus;
import tech.safepay.dtos.cards.*;
import tech.safepay.entities.Card;
import tech.safepay.exceptions.card.CardBlockedOrLostException;
import tech.safepay.exceptions.card.CardNotFoundException;
import tech.safepay.exceptions.card.CardQuantityMaxException;
import tech.safepay.exceptions.device.DeviceNotFoundException;
import tech.safepay.generator.DefaultCardGenerator;
import tech.safepay.repositories.CardRepository;
import tech.safepay.repositories.DeviceRepository;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

/**
 * Serviço responsável pela gestão completa do ciclo de vida de cartões de crédito.
 * <p>
 * Este serviço oferece operações essenciais para o gerenciamento de cartões, incluindo:
 * <ul>
 *   <li>Registro e criação de novos cartões em lote</li>
 *   <li>Exclusão de cartões existentes</li>
 *   <li>Consulta de cartões por dispositivo</li>
 *   <li>Consulta de detalhes de cartões individuais</li>
 *   <li>Reset de limites de crédito disponíveis</li>
 *   <li>Listagem paginada com filtros avançados</li>
 * </ul>
 * <p>
 * O serviço implementa regras de negócio críticas como limitação de quantidade de cartões,
 * mascaramento de números sensíveis e validações de segurança.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
@Transactional
public class CardService {

    /**
     * Limite máximo de cartões que podem existir no sistema simultaneamente.
     * Esta restrição previne sobrecarga do banco de dados e garante performance adequada.
     */
    private static final Integer QUANTITY_LIMIT = 500;

    /**
     * Gerador de números aleatórios thread-safe utilizado em operações que requerem aleatoriedade.
     */
    private static final Random RANDOM = new Random();

    private final DefaultCardGenerator defaultCardGenerator;
    private final CardRepository cardRepository;
    private final DeviceRepository deviceRepository;

    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param defaultCardGenerator gerador de dados padrão para criação de cartões
     * @param cardRepository repositório para operações de persistência de cartões
     * @param deviceRepository repositório para operações de persistência de dispositivos
     */
    public CardService(DefaultCardGenerator defaultCardGenerator, CardRepository cardRepository, DeviceRepository deviceRepository) {
        this.defaultCardGenerator = defaultCardGenerator;
        this.cardRepository = cardRepository;
        this.deviceRepository = deviceRepository;
    }

    /**
     * Registra um lote de novos cartões de crédito no sistema.
     * <p>
     * Este método cria múltiplos cartões de uma só vez, cada um com:
     * <ul>
     *   <li>Bandeira aleatória (Visa, Mastercard, etc.)</li>
     *   <li>Número de cartão válido gerado automaticamente</li>
     *   <li>Data de expiração futura</li>
     *   <li>Score de risco calculado</li>
     *   <li>Limite de crédito atribuído</li>
     *   <li>Status ativo por padrão</li>
     *   <li>Nome de titular gerado</li>
     * </ul>
     * <p>
     * <b>Regras de negócio:</b>
     * <ul>
     *   <li>A quantidade total de cartões no sistema não pode exceder {@value #QUANTITY_LIMIT}</li>
     *   <li>Cada cartão é criado com limite disponível igual ao limite total</li>
     *   <li>Todos os cartões são criados com status ACTIVE</li>
     * </ul>
     *
     * @param quantity número de cartões a serem criados (deve ser ≤ {@value #QUANTITY_LIMIT})
     * @return CardResponse com status HTTP CREATED e mensagem de confirmação
     * @throws CardQuantityMaxException se o limite máximo de cartões no sistema for atingido
     */
    public CardResponse cardRegister(Integer quantity){

        if (cardRepository.findAll().size() >= 500){
            throw new CardQuantityMaxException("Número máximo de criações alcançados!");
        }

        for (int i = 0; i < quantity; i++) {
            Card card = new Card();

            var cardBrand = defaultCardGenerator.choiceCardBrand();


            card.setCardBrand(cardBrand);
            card.setCardNumber(defaultCardGenerator.generateNumber(cardBrand));
            card.setCreatedAt(LocalDateTime.now());
            card.setExpirationDate(defaultCardGenerator.generateExpirationDate());
            card.setRiskScore(defaultCardGenerator.generateRiskScore());

            var credit = defaultCardGenerator.generateCreditLimit();
            card.setCreditLimit(credit);
            card.setRemainingLimit(credit);


            card.setStatus(CardStatus.ACTIVE);
            card.setCardHolderName(defaultCardGenerator.generateName());
            cardRepository.saveAndFlush(card);

        }

        return new CardResponse(
                HttpStatus.CREATED,
                "Cartões criados"
        );
    }


    /**
     * Remove permanentemente um cartão do sistema.
     * <p>
     * Esta operação é irreversível e deve ser utilizada com cautela.
     * O cartão e todos os seus dados associados serão excluídos do banco de dados.
     * <p>
     * <b>Importante:</b> Considere desativar o cartão (alterando status) ao invés de
     * excluí-lo, caso seja necessário manter histórico ou rastreabilidade.
     *
     * @param id identificador único do cartão a ser excluído
     * @return CardResponse com status HTTP OK e mensagem de confirmação
     * @throws CardNotFoundException se o cartão com o ID especificado não existir
     */
    public CardResponse cardDelete(UUID id){
        Card card = cardRepository.findById(id).orElseThrow(() -> new CardNotFoundException("Cartão não encontrado"));
        cardRepository.delete(card);
        return new CardResponse(
                HttpStatus.OK,
                "Cartão deletado com sucesso!"
        );
    }


    /**
     * Aplica máscara de segurança ao número do cartão, ocultando dígitos sensíveis.
     * <p>
     * Exibe apenas os últimos 4 dígitos do cartão, substituindo os demais por asteriscos.
     * Este padrão segue práticas de segurança PCI-DSS para proteção de dados de cartão.
     * <p>
     * <b>Exemplos:</b>
     * <ul>
     *   <li>Entrada: "1234567890123456" → Saída: "**** **** **** 3456"</li>
     *   <li>Entrada: "123" → Saída: "****" (cartão inválido)</li>
     *   <li>Entrada: null → Saída: "****"</li>
     * </ul>
     *
     * @param cardNumber número completo do cartão (16 dígitos)
     * @return string com número mascarado exibindo apenas os últimos 4 dígitos
     */
    private String getMaskedCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * Recupera a lista de todos os cartões vinculados a um dispositivo específico.
     * <p>
     * Este método retorna informações resumidas de cada cartão associado ao dispositivo,
     * incluindo número mascarado, titular, bandeira, validade e limites.
     * <p>
     * Os números de cartão são automaticamente mascarados por questões de segurança,
     * exibindo apenas os últimos 4 dígitos.
     *
     * @param deviceId identificador único do dispositivo
     * @return CardsInDeviceResponseDto contendo lista de DTOs com dados dos cartões vinculados
     * @throws DeviceNotFoundException se o dispositivo com o ID especificado não existir
     */
    public CardsInDeviceResponseDto getCardsInDevice(UUID deviceId) {

        var device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Dispositivo não encontrado!"));

        var cardDtos = device.getCards().stream()
                .map(card -> new CardsInDeviceResponseDto.CardResponseDto(
                        card.getCardId(),
                        getMaskedCardNumber(card.getCardNumber()),
                        card.getCardHolderName(),
                        card.getCardBrand(),
                        card.getExpirationDate(),
                        card.getCreditLimit(),
                        card.getStatus()
                ))
                .toList();

        return new CardsInDeviceResponseDto(cardDtos);
    }


    /**
     * Recupera os dados detalhados de um cartão específico.
     * <p>
     * Retorna informações completas do cartão incluindo:
     * <ul>
     *   <li>Identificador único</li>
     *   <li>Número mascarado (apenas últimos 4 dígitos visíveis)</li>
     *   <li>Nome do titular</li>
     *   <li>Bandeira do cartão</li>
     *   <li>Data de expiração</li>
     *   <li>Limite de crédito total</li>
     *   <li>Status atual</li>
     * </ul>
     * <p>
     * Por razões de segurança, o número completo do cartão nunca é exposto.
     *
     * @param cardId identificador único do cartão
     * @return CardDataResponseDto contendo os dados detalhados do cartão
     * @throws CardNotFoundException se o cartão com o ID especificado não existir
     */
    public CardDataResponseDto getCardById(UUID cardId) {

        var card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Cartão não encontrado"));

        return new CardDataResponseDto(
                card.getCardId(),
                getMaskedCardNumber(card.getCardNumber()),
                card.getCardHolderName(),
                card.getCardBrand(),
                card.getExpirationDate(),
                card.getCreditLimit(),
                card.getStatus()
        );
    }


    /**
     * Restaura o limite de crédito disponível de todos os cartões para o valor original.
     * <p>
     * Esta operação é útil para:
     * <ul>
     *   <li>Reset periódico de limites (ciclo mensal)</li>
     *   <li>Manutenção do sistema</li>
     *   <li>Testes e simulações</li>
     * </ul>
     * <p>
     * Para cada cartão, o campo <code>remainingLimit</code> é restaurado para o valor
     * de <code>creditLimit</code>, efetivamente "limpando" todos os gastos anteriores.
     * <p>
     * <b>Atenção:</b> Esta operação afeta TODOS os cartões do sistema e deve ser
     * executada com cuidado em ambientes de produção.
     *
     * @return CardResponse com status HTTP OK e mensagem informando o resultado da operação
     */
    public CardResponse resetRemainingCreditAllCards() {
        var cards = cardRepository.findAll();

        if (cards.isEmpty()) {
            return new CardResponse(
                    HttpStatus.OK,
                    "Não há cartões para resetar."
            );
        }

        for (Card card : cards) {
            card.setRemainingLimit(card.getCreditLimit());
        }

        cardRepository.saveAll(cards);
        cardRepository.flush(); // garante persistência imediata

        return new CardResponse(
                HttpStatus.OK,
                "Todos os cartões foram resetados!"
        );
    }

    /**
     * Recupera uma lista paginada de cartões com filtros opcionais aplicados.
     * <p>
     * Este método suporta os seguintes filtros:
     * <ul>
     *   <li><b>cardBrand:</b> filtra por bandeira específica (Visa, Mastercard, etc.)</li>
     *   <li><b>recentlyCreated:</b> quando true, retorna apenas cartões criados nos últimos 7 dias</li>
     * </ul>
     * <p>
     * Os resultados são ordenados por data de criação em ordem decrescente (mais recentes primeiro)
     * e retornados em formato paginado para otimizar performance e experiência do usuário.
     * <p>
     * <b>Paginação:</b>
     * <ul>
     *   <li>A primeira página tem índice 0</li>
     *   <li>O tamanho da página controla quantos registros são retornados</li>
     *   <li>A resposta inclui metadados de paginação (total de páginas, total de elementos, etc.)</li>
     * </ul>
     *
     * @param cardBrand bandeira do cartão para filtrar (opcional, pode ser null)
     * @param recentlyCreated se true, filtra cartões criados nos últimos 7 dias (opcional, pode ser null)
     * @param page número da página a ser recuperada (zero-based)
     * @param size quantidade de registros por página
     * @return Page&lt;CardsResponse&gt; contendo os cartões que atendem aos critérios especificados
     */
    public Page<CardsResponse> getWithFilters(
            CardBrand cardBrand,
            Boolean recentlyCreated,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        LocalDateTime limitDate = LocalDateTime.now().minusDays(7);

        return cardRepository
                .findWithFilters(cardBrand, recentlyCreated, limitDate, pageable)
                .map(CardsResponse::fromEntity);
    }




    /**
     * Atualiza o estado de um cartão (bloqueado ou perdido) se estiver vinculado ao deviceId informado.
     *
     * <p>O cartão só será atualizado se existir e se o {@code deviceId} fornecido
     * corresponder ao dispositivo registrado no cartão.</p>
     *
     * @param cardId UUID do cartão a ser atualizado
     * @param deviceId UUID do dispositivo vinculado ao cartão
     * @param block Se true, marca como bloqueado; se false, marca como perdido
     * @return {@link CardBlockResponseDto} com o resultado da operação
     * @throws CardBlockedOrLostException se o cartão já estiver bloqueado ou perdido
     * @throws CardNotFoundException se o cartão não existir ou não estiver vinculado ao device informado
     */
    public CardBlockResponseDto updateCardStatus(UUID cardId, UUID deviceId, boolean block) {
        var cardOptional = cardRepository.findByCardIdAndDevices_Id(cardId, deviceId);

        var card = cardOptional.orElseThrow(() -> new CardNotFoundException("Cartão não encontrado para este dispositivo."));

        if (card.getStatus().equals(CardStatus.BLOCKED) || card.getStatus().equals(CardStatus.LOST)) {
            throw new CardBlockedOrLostException("Cartão já está bloqueado ou perdido.");
        }

        if (block) {
            card.setStatus(CardStatus.BLOCKED);
        } else {
            card.setStatus(CardStatus.LOST);
        }

        cardRepository.saveAndFlush(card);

        String message = block ? "Cartão bloqueado com sucesso" : "Cartão marcado como perdido com sucesso";

        return new CardBlockResponseDto(
                card.getCardId(),
                deviceId,
                true,
                message
        );
    }



}