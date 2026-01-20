package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findByCard_CardId(UUID cardId);

    List<Transaction> findTop20ByCardOrderByCreatedAtDesc(Card card);

    List<Transaction> findByCardAndCreatedAtAfter(
            Card card,
            LocalDateTime createdAt
    );

    Optional<Transaction> findFirstByCardOrderByCreatedAtDesc(Card card);


    List<Transaction> findTop10ByCardOrderByCreatedAtDesc(Card card);

    List<Transaction> findTop5ByCardOrderByCreatedAtDesc(Card card);
}
