package tech.safepay.validations;

import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tech.safepay.entities.Card;
import tech.safepay.entities.Transaction;
import tech.safepay.repositories.TransactionRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Contexto de validação com escopo de requisição para gerenciamento eficiente de histórico.
 * <p>
 * Esta classe atua como um cache de escopo de requisição HTTP, carregando e mantendo
 * o histórico de transações necessário para todas as validações durante o processamento
 * de uma única transação. O carregamento é realizado uma única vez por requisição,
 * evitando múltiplas consultas ao banco de dados.
 * </p>
 * <p>
 * <strong>Arquitetura de Cache:</strong>
 * <ol>
 *   <li><strong>Consulta Única:</strong> Busca as últimas 20 transações do cartão uma vez</li>
 *   <li><strong>Derivações em Memória:</strong> Filtra diferentes janelas temporais a partir do resultado</li>
 *   <li><strong>Escopo de Requisição:</strong> Mantém dados durante toda a requisição HTTP</li>
 *   <li><strong>Lazy Loading:</strong> Carrega dados apenas quando {@link #loadContext(Transaction)} é chamado</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Janelas Temporais Disponíveis:</strong>
 * <ul>
 *   <li><strong>last20Transactions:</strong> Últimas 20 transações do cartão (ordenadas por data decrescente)</li>
 *   <li><strong>last10Transactions:</strong> Últimas 10 transações do cartão</li>
 *   <li><strong>last24HoursTransactions:</strong> Transações das últimas 24 horas</li>
 *   <li><strong>last5MinutesTransactions:</strong> Transações dos últimos 5 minutos</li>
 *   <li><strong>last10MinutesTransactions:</strong> Transações dos últimos 10 minutos</li>
 * </ul>
 * </p>
 * <p>
 * <strong>IMPORTANTE - Configuração de Escopo:</strong>
 * <br>
 * Esta classe DEVE permanecer com {@code @RequestScope} e {@code proxyMode = TARGET_CLASS}.
 * Remover estas anotações quebrará o algoritmo de validação, pois as validações paralelas
 * dependem do contexto carregado na thread HTTP principal antes da execução assíncrona.
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)     // NÃO REMOVER, SE REMOVER O ALGORITMO DE VALIDAÇÃO QUEBRA!
public class ValidationContext {

    /**
     * Repositório para acesso às transações do banco de dados.
     */
    private final TransactionRepository transactionRepository;

    /**
     * Flag indicando se o contexto já foi carregado nesta requisição.
     * <p>
     * Previne carregamento duplicado e garante que os dados sejam buscados apenas uma vez,
     * mesmo se {@link #loadContext(Transaction)} for chamado múltiplas vezes.
     * </p>
     */
    private boolean loaded = false;

    /**
     * Cache das últimas 20 transações do cartão.
     * <p>
     * Esta é a consulta base da qual todas as outras janelas temporais são derivadas.
     * </p>
     */
    private List<Transaction> last20Transactions = Collections.emptyList();

    /**
     * Cache das últimas 10 transações do cartão.
     * <p>
     * Derivada de {@link #last20Transactions} através de limitação em memória.
     * </p>
     */
    private List<Transaction> last10Transactions = Collections.emptyList();

    /**
     * Cache de transações das últimas 24 horas.
     * <p>
     * Derivada de {@link #last20Transactions} através de filtro temporal em memória.
     * </p>
     */
    private List<Transaction> last24HoursTransactions = Collections.emptyList();

    /**
     * Cache de transações dos últimos 5 minutos.
     * <p>
     * Derivada de {@link #last20Transactions} através de filtro temporal em memória.
     * </p>
     */
    private List<Transaction> last5MinutesTransactions = Collections.emptyList();

    /**
     * Cache de transações dos últimos 10 minutos.
     * <p>
     * Derivada de {@link #last20Transactions} através de filtro temporal em memória.
     * </p>
     */
    private List<Transaction> last10MinutesTransactions = Collections.emptyList();

    /**
     * Construtor para injeção de dependências.
     *
     * @param transactionRepository repositório de transações para consultas ao banco
     */
    public ValidationContext(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Carrega o contexto de validação para a transação especificada.
     * <p>
     * Este método executa uma única consulta ao banco de dados para recuperar as últimas
     * 20 transações do cartão e, a partir desse resultado, deriva todas as janelas temporais
     * necessárias através de operações de filtro em memória.
     * </p>
     * <p>
     * <strong>Estratégia de Otimização:</strong>
     * <ul>
     *   <li><strong>Consulta Única:</strong> Busca apenas as últimas 20 transações</li>
     *   <li><strong>Filtros em Memória:</strong> Todas as outras janelas são derivadas sem novas consultas</li>
     *   <li><strong>Carregamento Único:</strong> Executa apenas uma vez por requisição (idempotente)</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Processo de Carregamento:</strong>
     * <ol>
     *   <li>Verifica se já foi carregado (early return se sim)</li>
     *   <li>Valida disponibilidade de transação e cartão</li>
     *   <li>Define tempo de referência (timestamp da transação ou agora)</li>
     *   <li>Executa consulta única das últimas 20 transações</li>
     *   <li>Deriva last10 (limita a 10 elementos)</li>
     *   <li>Deriva last24Hours (filtra por timestamp das últimas 24h)</li>
     *   <li>Deriva last5Minutes (filtra por timestamp dos últimos 5min)</li>
     *   <li>Deriva last10Minutes (filtra por timestamp dos últimos 10min)</li>
     *   <li>Marca como carregado</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Tratamento de Casos Especiais:</strong>
     * <ul>
     *   <li>Se transação for nula: marca como carregado e mantém listas vazias</li>
     *   <li>Se cartão for nulo: marca como carregado e mantém listas vazias</li>
     *   <li>Se timestamp não disponível: usa LocalDateTime.now() como referência</li>
     *   <li>Transações sem timestamp: são ignoradas nos filtros temporais</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Garantias de Comportamento:</strong>
     * <ul>
     *   <li><strong>Idempotência:</strong> Múltiplas chamadas não recarregam dados</li>
     *   <li><strong>Thread-Safety:</strong> Executado na thread HTTP antes de validações paralelas</li>
     *   <li><strong>Imutabilidade:</strong> Listas retornadas são imutáveis após carregamento</li>
     * </ul>
     * </p>
     *
     * @param transaction a transação sendo validada, usada para identificar o cartão
     *                    e determinar o tempo de referência para filtros temporais
     * @see TransactionRepository#findTop20ByCardOrderByCreatedAtDesc(Card)
     */
    public void loadContext(Transaction transaction) {
        if (loaded) return;

        if (transaction == null || transaction.getCard() == null) {
            loaded = true;
            return;
        }

        Card card = transaction.getCard();

        LocalDateTime referenceTime =
                transaction.getCreatedAt() != null
                        ? transaction.getCreatedAt()
                        : LocalDateTime.now();

        // QUERY ÚNICA
        last20Transactions =
                transactionRepository.findTop20ByCardOrderByCreatedAtDesc(card);

        // DERIVAÇÕES EM MEMÓRIA
        last10Transactions =
                last20Transactions.stream().limit(10).toList();

        last24HoursTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusHours(24)))
                .toList();

        last5MinutesTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusMinutes(5)))
                .toList();

        last10MinutesTransactions = last20Transactions.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> t.getCreatedAt().isAfter(referenceTime.minusMinutes(10)))
                .toList();

        loaded = true;
    }

    // ========================================
    // GETTERS - ZERO LÓGICA DE NEGÓCIO
    // ========================================

    /**
     * Retorna as últimas 20 transações do cartão.
     * <p>
     * Esta é a lista base carregada diretamente do banco de dados,
     * ordenada por data de criação decrescente.
     * </p>
     *
     * @return lista imutável das últimas 20 transações, ou lista vazia se não carregado
     */
    public List<Transaction> getLast20Transactions() {
        return last20Transactions;
    }

    /**
     * Retorna as últimas 10 transações do cartão.
     * <p>
     * Derivada de {@link #getLast20Transactions()} através de limitação.
     * </p>
     *
     * @return lista imutável das últimas 10 transações, ou lista vazia se não carregado
     */
    public List<Transaction> getLast10Transactions() {
        return last10Transactions;
    }

    /**
     * Retorna transações das últimas 24 horas.
     * <p>
     * Derivada de {@link #getLast20Transactions()} através de filtro temporal.
     * </p>
     *
     * @return lista imutável de transações das últimas 24 horas, ou lista vazia se não carregado
     */
    public List<Transaction> getLast24Hours() {
        return last24HoursTransactions;
    }

    /**
     * Retorna transações dos últimos 5 minutos.
     * <p>
     * Derivada de {@link #getLast20Transactions()} através de filtro temporal.
     * </p>
     *
     * @return lista imutável de transações dos últimos 5 minutos, ou lista vazia se não carregado
     */
    public List<Transaction> getLast5Minutes() {
        return last5MinutesTransactions;
    }

    /**
     * Retorna transações dos últimos 10 minutos.
     * <p>
     * Derivada de {@link #getLast20Transactions()} através de filtro temporal.
     * </p>
     *
     * @return lista imutável de transações dos últimos 10 minutos, ou lista vazia se não carregado
     */
    public List<Transaction> getLast10Minutes() {
        return last10MinutesTransactions;
    }
}