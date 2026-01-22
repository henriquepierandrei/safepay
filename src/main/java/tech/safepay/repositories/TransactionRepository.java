package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Retorna todas as transações associadas a um cartão específico,
     * utilizando o identificador do cartão.
     *
     * Uso comum:
     * - Histórico completo do cartão
     * - Auditoria
     *
     * Atenção:
     * - Pode retornar grandes volumes de dados
     */
    List<Transaction> findByCard_CardId(UUID cardId);

    /**
     * Retorna as 20 transações mais recentes de um cartão,
     * ordenadas da mais nova para a mais antiga.
     *
     * Uso comum:
     * - Cálculo de média de valores
     * - Validações baseadas em histórico recente
     * - Análise de comportamento
     */
    List<Transaction> findTop20ByCardOrderByCreatedAtDesc(Card card);

    /**
     * Retorna todas as transações de um cartão criadas após
     * uma determinada data/hora.
     *
     * Uso comum:
     * - Detecção de rajadas de transações
     * - Análise por janela de tempo
     */
    List<Transaction> findByCardAndCreatedAtAfter(
            Card card,
            LocalDateTime createdAt
    );

    /**
     * Retorna a transação mais recente de um cartão.
     *
     * Uso comum:
     * - Comparação com a última transação
     *
     * Atenção:
     * - Se chamada após salvar a transação atual,
     *   retornará a própria transação recém-criada
     */
    Optional<Transaction> findFirstByCardOrderByCreatedAtDesc(Card card);

    /**
     * Retorna as 10 transações mais recentes de um cartão,
     * ordenadas da mais nova para a mais antiga.
     *
     * Uso comum:
     * - Validações rápidas
     * - Análises de curto prazo
     */
    List<Transaction> findTop10ByCardOrderByCreatedAtDesc(Card card);

    /**
     * Retorna as 5 transações mais recentes de um cartão,
     * ordenadas da mais nova para a mais antiga.
     *
     * Uso comum:
     * - Validações extremamente sensíveis à recência
     * - Heurísticas rápidas
     */
    List<Transaction> findTop5ByCardOrderByCreatedAtDesc(Card card);

    /**
     * Retorna a transação mais recente de um cartão,
     * EXCLUINDO uma transação específica.
     *
     * Uso comum:
     * - Validações comparativas (ex: impossible travel)
     * - Evitar comparar a transação atual com ela mesma
     *
     * Esta é a forma correta de buscar a "transação anterior"
     * em pipelines síncronos.
     */
    Optional<Transaction> findFirstByCardAndTransactionIdNotOrderByCreatedAtDesc(
            Card card,
            UUID transactionId
    );


    boolean existsByDevice(Device device);

    long countByDevice(Device device);

    Optional<Transaction> findFirstByDeviceAndTransactionIdNotOrderByCreatedAtDesc(
            Device device,
            UUID transactionId
    );

    List<Transaction> findByCard(Card card);


}
