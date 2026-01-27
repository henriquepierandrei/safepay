package tech.safepay.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.safepay.Enums.CardBrand;
import tech.safepay.entities.Card;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    List<Card> findByDevicesIsNotEmpty();

    @Query("""
                SELECT c FROM Card c
                WHERE (:brand IS NULL OR c.cardBrand = :brand)
                  AND (
                        :recentlyCreated IS NULL
                        OR (
                            :recentlyCreated = true AND c.createdAt >= :limitDate
                        )
                        OR (
                            :recentlyCreated = false AND c.createdAt < :limitDate
                        )
                      )
            """)
    Page<Card> findWithFilters(
            @Param("brand") CardBrand brand,
            @Param("recentlyCreated") Boolean recentlyCreated,
            @Param("limitDate") LocalDateTime limitDate,
            Pageable pageable
    );


}
