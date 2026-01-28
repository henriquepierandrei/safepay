package tech.safepay.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.safepay.entities.Card;
import tech.safepay.entities.Device;
import tech.safepay.entities.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório responsável pelo acesso e persistência
 * de transações financeiras no sistema SafePay.
 *
 * <p>Centraliza consultas relacionadas ao histórico
 * transacional de cartões, sendo amplamente utilizado
 * em fluxos de validação antifraude, análise de risco,
 * cálculo de métricas comportamentais e auditoria.</p>
 *
 * <p>As consultas aqui definidas priorizam performance
 * e previsibilidade, evitando sobrecarga em cenários
 * de alto volume transacional.</p>
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /**
     * Retorna as 20 transações mais recentes associadas a um cartão,
     * ordenadas da mais recente para a mais antiga.
     *
     * <p>Este método é amplamente utilizado para:</p>
     * <ul>
     *   <li>Cálculo de médias e desvios de valor</li>
     *   <li>Detecção de padrões suspeitos recentes</li>
     *   <li>Validações baseadas em comportamento histórico</li>
     *   <li>Análises de frequência e velocidade transacional</li>
     * </ul>
     *
     * @param card cartão cujas transações recentes serão consultadas
     * @return lista contendo até 20 transações mais recentes do cartão
     */
    List<Transaction> findTop20ByCardOrderByCreatedAtDesc(Card card);

    /**
     * Retorna todas as transações associadas a um cartão específico.
     *
     * <p>Utilizado principalmente em:</p>
     * <ul>
     *   <li>Consultas completas de histórico</li>
     *   <li>Relatórios e auditorias</li>
     *   <li>Reconciliações e análises aprofundadas</li>
     * </ul>
     *
     * @param card cartão a ser utilizado como critério de busca
     * @return lista completa de transações vinculadas ao cartão
     */
    List<Transaction> findByCard(Card card);



    Page<Transaction> findTransactionsByCardAndDevice(
            UUID cardId,
            UUID deviceId,
            Boolean isReimbursement,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );



}
