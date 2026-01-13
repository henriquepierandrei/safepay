package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.CardPattern;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardPatternRepository extends JpaRepository<CardPattern, UUID> {
    Optional<CardPattern> findByCard_CardId(UUID cardId);
}
