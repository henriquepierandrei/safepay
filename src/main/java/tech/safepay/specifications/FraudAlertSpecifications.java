package tech.safepay.specifications;

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

/**
 * Classe utilitária responsável por centralizar as especificações
 * dinâmicas de consulta da entidade {@link FraudAlert}.
 *
 * <p>Este componente segue o padrão {@link Specification} do Spring Data JPA,
 * permitindo a construção de filtros flexíveis e combináveis sem acoplamento
 * direto ao repositório.</p>
 *
 * <p>É utilizada principalmente em cenários de:</p>
 * <ul>
 *   <li>Dashboards analíticos</li>
 *   <li>Listagens paginadas com múltiplos filtros</li>
 *   <li>Consultas operacionais e antifraude</li>
 * </ul>
 *
 * <p>A abordagem garante escalabilidade, manutenibilidade e clareza
 * na evolução das regras de busca.</p>
 */
public class FraudAlertSpecifications {

    /**
     * Constrói uma {@link Specification} para filtrar alertas de fraude
     * com base em múltiplos critérios opcionais.
     *
     * <p>Todos os parâmetros são opcionais. Apenas os filtros informados
     * serão aplicados à consulta final.</p>
     *
     * <p>Os filtros suportados incluem:</p>
     * <ul>
     *   <li>Alertas recentes (últimas 24 horas)</li>
     *   <li>Severidade do alerta</li>
     *   <li>Faixa de score de fraude</li>
     *   <li>Tipos de alerta associados</li>
     *   <li>Data mínima de criação</li>
     *   <li>Identificação da transação</li>
     *   <li>Identificação do cartão</li>
     *   <li>Identificação do dispositivo associado ao cartão</li>
     * </ul>
     *
     * <p>O uso de {@code JOINs} é realizado apenas quando necessário,
     * evitando impacto desnecessário na performance da consulta.</p>
     *
     * @param recentAlerts      indica se devem ser retornados apenas alertas das últimas 24 horas
     * @param severity          severidade específica do alerta
     * @param startFraudScore   score mínimo de fraude
     * @param endFraudScore     score máximo de fraude
     * @param alertTypeList     lista de tipos de alerta a serem considerados
     * @param createdAtFrom     data mínima de criação do alerta
     * @param transactionId     identificador da transação associada
     * @param deviceId          identificador do dispositivo vinculado ao cartão
     * @param cardId            identificador do cartão associado
     * @return {@link Specification} configurada conforme os filtros informados
     */
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

            // Filtro: tipos de alerta (busca textual em campo serializado)
            if (alertTypeList != null && !alertTypeList.isEmpty()) {
                List<Predicate> alertTypePredicates = new ArrayList<>();
                for (AlertType alertType : alertTypeList) {
                    alertTypePredicates.add(
                            criteriaBuilder.like(root.get("alertTypes"), "%" + alertType.name() + "%")
                    );
                }
                predicates.add(criteriaBuilder.or(alertTypePredicates.toArray(new Predicate[0])));
            }

            // Filtro: data de criação mínima
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

            // Filtro: deviceId (via relacionamento card -> devices)
            if (deviceId != null) {
                Join<FraudAlert, Card> cardJoin = root.join("card");
                Join<Object, Object> devicesJoin = cardJoin.join("devices");
                predicates.add(criteriaBuilder.equal(devicesJoin.get("id"), deviceId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
