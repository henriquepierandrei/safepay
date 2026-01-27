package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.Card;
import tech.safepay.entities.CardPattern;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório responsável pelo acesso e gerenciamento
 * dos padrões comportamentais associados a cartões.
 *
 * <p>Centraliza operações de persistência e consulta
 * relacionadas ao perfil transacional de um {@link Card},
 * permitindo análises de comportamento e detecção de
 * anomalias.</p>
 *
 * <p>Baseado no {@link JpaRepository}, oferece suporte
 * completo a operações CRUD e consultas derivadas
 * por convenção de nomes.</p>
 */
@Repository
public interface CardPatternRepository extends JpaRepository<CardPattern, UUID> {

    /**
     * Recupera o padrão comportamental associado a um cartão específico.
     *
     * <p>Utilizado para avaliar desvios de comportamento,
     * recorrência de transações e consistência de perfil
     * durante o processamento antifraude.</p>
     *
     * @param card cartão de referência
     * @return {@link Optional} contendo o {@link CardPattern} associado,
     *         ou vazio caso não exista padrão cadastrado
     */
    Optional<CardPattern> findByCard(Card card);
}
