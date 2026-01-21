package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.Card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    Optional<Card> findByCardNumber(String cardNumber);

    List<Card> findByDevicesIsNotEmpty();

}
