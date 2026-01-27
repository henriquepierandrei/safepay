package tech.safepay.configs;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.entities.Card;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FraudAlertSpecifications {

    public static Specification<FraudAlert> withFilters(
            Boolean recentAlerts,
            Severity severity,
            Integer startFraudScore,
            Integer endFraudScore,
            List<AlertType> alertTypeList,
            LocalDateTime createdAtFrom,
            UUID transactionId,
            UUID deviceId,
            UUID cardId
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtro: alertas recentes (últimas 24h)
            if (recentAlerts != null && recentAlerts) {
                LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), last24Hours));
            }

            // Filtro: severidade
            if (severity != null) {
                predicates.add(criteriaBuilder.equal(root.get("severity"), severity));
            }

            // Filtro: faixa de fraud score
            if (startFraudScore != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fraudScore"), startFraudScore));
            }
            if (endFraudScore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fraudScore"), endFraudScore));
            }

            // Filtro: tipos de alerta (usando LIKE para buscar na string delimitada)
            if (alertTypeList != null && !alertTypeList.isEmpty()) {
                List<Predicate> alertTypePredicates = new ArrayList<>();
                for (AlertType alertType : alertTypeList) {
                    alertTypePredicates.add(
                            criteriaBuilder.like(root.get("alertTypes"), "%" + alertType.name() + "%")
                    );
                }
                predicates.add(criteriaBuilder.or(alertTypePredicates.toArray(new Predicate[0])));
            }

            // Filtro: data de criação a partir de
            if (createdAtFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom));
            }

            // Filtro: transactionId
            if (transactionId != null) {
                Join<FraudAlert, Transaction> transactionJoin = root.join("transaction");
                predicates.add(criteriaBuilder.equal(transactionJoin.get("transactionId"), transactionId));
            }

            // Filtro: cardId
            if (cardId != null) {
                Join<FraudAlert, Card> cardJoin = root.join("card");
                predicates.add(criteriaBuilder.equal(cardJoin.get("cardId"), cardId));
            }

            // Filtro: deviceId (via card -> devices)
            if (deviceId != null) {
                Join<FraudAlert, Card> cardJoin = root.join("card");
                Join<Object, Object> devicesJoin = cardJoin.join("devices");
                predicates.add(criteriaBuilder.equal(devicesJoin.get("id"), deviceId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}