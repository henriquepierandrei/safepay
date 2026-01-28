package tech.safepay.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.safepay.entities.FraudAlert;
import tech.safepay.entities.Transaction;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório responsável pela persistência e consulta
 * de alertas de fraude gerados pelo motor antifraude.
 *
 * <p>Este repositório oferece suporte tanto a operações
 * CRUD padrão quanto a consultas dinâmicas baseadas em
 * {@link org.springframework.data.jpa.domain.Specification},
 * permitindo a construção de filtros complexos e
 * altamente configuráveis.</p>
 *
 * <p>É amplamente utilizado em fluxos de análise,
 * monitoramento, investigação manual e auditoria
 * de eventos suspeitos.</p>
 */
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID>,
        JpaSpecificationExecutor<FraudAlert> {
    Optional<FraudAlert> findByTransaction(Transaction transaction);
}
