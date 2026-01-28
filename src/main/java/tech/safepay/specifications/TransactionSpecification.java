package tech.safepay.specifications;

import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
public class TransactionSpecification {

    public static Specification<Transaction> filter(
            UUID cardId,
            UUID deviceId,
            Boolean isReimbursement,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        return (root, query, cb) -> {

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            Join<Transaction, Card> cardJoin = root.join("card");
            Join<Card, Device> deviceJoin = cardJoin.join("devices");

            // ✅ aqui está o fix
            predicates.add(cb.equal(cardJoin.get("cardId"), cardId));
            predicates.add(cb.equal(deviceJoin.get("id"), deviceId));

            if (isReimbursement != null) {
                predicates.add(
                        cb.equal(root.get("isReimbursement"), isReimbursement)
                );
            }

            if (startDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startDate)
                );
            }

            if (endDate != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(root.get("createdAt"), endDate)
                );
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

}
