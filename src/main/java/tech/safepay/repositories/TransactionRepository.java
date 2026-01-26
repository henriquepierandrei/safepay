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
     * Retorna as 20 transações mais recentes de um cartão,
     * ordenadas da mais nova para a mais antiga.
     *
     * Uso comum:
     * - Cálculo de média de valores
     * - Validações baseadas em histórico recente
     * - Análise de comportamento
     */
    List<Transaction> findTop20ByCardOrderByCreatedAtDesc(Card card);

    List<Transaction> findByCard(Card card);

}
