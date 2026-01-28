package tech.safepay.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import tech.safepay.Enums.AlertType;
import tech.safepay.Enums.Severity;
import tech.safepay.specifications.FraudAlertSpecifications;
import tech.safepay.dtos.fraudalert.FraudAlertResponseDTO;
import tech.safepay.entities.FraudAlert;
import tech.safepay.repositories.FraudAlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pela consulta e recuperação de alertas de fraude.
 * <p>
 * Este serviço oferece funcionalidades para acesso aos alertas de fraude gerados
 * pelo sistema antifraude, permitindo:
 * <ul>
 *   <li>Consultas paginadas com múltiplos filtros combinados</li>
 *   <li>Ordenação cronológica decrescente (alertas mais recentes primeiro)</li>
 *   <li>Filtragem por severidade, score, tipos de alerta e entidades relacionadas</li>
 *   <li>Recuperação sem filtros para visão completa do sistema</li>
 * </ul>
 * <p>
 * O serviço atua como camada de abstração entre os controladores e o repositório,
 * utilizando Specifications do Spring Data JPA para construir queries dinâmicas
 * e complexas de forma type-safe.
 * <p>
 * <b>Casos de uso principais:</b>
 * <ul>
 *   <li>Dashboards de monitoramento de fraude em tempo real</li>
 *   <li>Ferramentas de análise e investigação de fraudes</li>
 *   <li>Relatórios de compliance e auditoria</li>
 *   <li>Sistemas de alerta e notificação</li>
 * </ul>
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Service
public class FraudAlertService {

    private final FraudAlertRepository fraudAlertRepository;

    /**
     * Construtor do serviço com injeção de dependências.
     *
     * @param fraudAlertRepository repositório para acesso aos alertas de fraude
     */
    public FraudAlertService(FraudAlertRepository fraudAlertRepository) {
        this.fraudAlertRepository = fraudAlertRepository;
    }

    /**
     * Recupera uma lista paginada de alertas de fraude aplicando múltiplos filtros combinados.
     * <p>
     * Este método suporta filtragem avançada por diversos critérios que podem ser combinados
     * livremente para criar queries complexas. Todos os filtros são opcionais (podem ser null).
     * <p>
     * <b>Filtros disponíveis:</b>
     * <table border="1">
     *   <tr>
     *     <th>Filtro</th>
     *     <th>Descrição</th>
     *     <th>Exemplo de uso</th>
     *   </tr>
     *   <tr>
     *     <td>recentAlerts</td>
     *     <td>Filtra alertas recentes (últimas 24h quando true)</td>
     *     <td>Dashboard de monitoramento em tempo real</td>
     *   </tr>
     *   <tr>
     *     <td>severity</td>
     *     <td>Filtra por nível de severidade específico</td>
     *     <td>Visualizar apenas alertas CRITICAL ou HIGH</td>
     *   </tr>
     *   <tr>
     *     <td>startFraudScore</td>
     *     <td>Score mínimo de fraude (threshold inferior)</td>
     *     <td>Alertas com score >= 70</td>
     *   </tr>
     *   <tr>
     *     <td>endFraudScore</td>
     *     <td>Score máximo de fraude (threshold superior)</td>
     *     <td>Alertas com score <= 85</td>
     *   </tr>
     *   <tr>
     *     <td>alertTypeList</td>
     *     <td>Filtra por tipos específicos de alerta</td>
     *     <td>Apenas VELOCITY_CHECK e HIGH_VALUE</td>
     *   </tr>
     *   <tr>
     *     <td>createdAtFrom</td>
     *     <td>Data/hora mínima de criação do alerta</td>
     *     <td>Alertas criados após 01/01/2025</td>
     *   </tr>
     *   <tr>
     *     <td>transactionId</td>
     *     <td>Filtra alertas de uma transação específica</td>
     *     <td>Investigação de transação suspeita</td>
     *   </tr>
     *   <tr>
     *     <td>deviceId</td>
     *     <td>Filtra alertas originados de um dispositivo</td>
     *     <td>Análise de comportamento por dispositivo</td>
     *   </tr>
     *   <tr>
     *     <td>cardId</td>
     *     <td>Filtra alertas relacionados a um cartão</td>
     *     <td>Histórico de fraudes de um cartão</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Comportamento de paginação:</b>
     * <ul>
     *   <li>Resultados ordenados por data de criação (mais recentes primeiro)</li>
     *   <li>Primeira página tem índice 0 (zero-based)</li>
     *   <li>Metadados de paginação incluídos na resposta (total de páginas, elementos, etc.)</li>
     * </ul>
     * <p>
     * <b>Performance:</b>
     * Este método utiliza JPA Specifications para construir queries otimizadas.
     * Filtros null são automaticamente ignorados, evitando condições desnecessárias.
     *
     * @param recentAlerts quando true, retorna apenas alertas das últimas 24 horas (opcional)
     * @param severity nível de severidade específico para filtrar (opcional)
     * @param startFraudScore score mínimo de fraude para inclusão (opcional)
     * @param endFraudScore score máximo de fraude para inclusão (opcional)
     * @param alertTypeList lista de tipos de alerta para filtrar (opcional)
     * @param createdAtFrom data/hora mínima de criação para filtrar (opcional)
     * @param transactionId ID da transação associada (opcional)
     * @param deviceId ID do dispositivo associado (opcional)
     * @param cardId ID do cartão associado (opcional)
     * @param page número da página a recuperar (zero-based)
     * @param size quantidade de registros por página
     * @return Page&lt;FraudAlertResponseDTO&gt; contendo os alertas que atendem aos critérios especificados
     */
    public Page<FraudAlertResponseDTO> getWithFilters(
            Boolean recentAlerts,
            Severity severity,
            Integer startFraudScore,
            Integer endFraudScore,
            List<AlertType> alertTypeList,
            LocalDateTime createdAtFrom,
            UUID transactionId,
            UUID deviceId,
            UUID cardId,
            int page,
            int size
    ) {
        Specification<FraudAlert> spec = FraudAlertSpecifications.withFilters(
                recentAlerts,
                severity,
                startFraudScore,
                endFraudScore,
                alertTypeList,
                createdAtFrom,
                transactionId,
                deviceId,
                cardId
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<FraudAlert> fraudAlerts = fraudAlertRepository.findAll(spec, pageable);

        return fraudAlerts.map(FraudAlertResponseDTO::from);
    }

    /**
     * Recupera todos os alertas de fraude sem aplicar filtros, apenas com paginação.
     * <p>
     * Este método é útil para:
     * <ul>
     *   <li>Visão geral completa do sistema de alertas</li>
     *   <li>Dashboards administrativos gerais</li>
     *   <li>Exportação de dados para análise externa</li>
     *   <li>Relatórios periódicos de compliance</li>
     * </ul>
     * <p>
     * <b>Características:</b>
     * <ul>
     *   <li>Retorna TODOS os alertas do sistema em ordem cronológica decrescente</li>
     *   <li>Nenhum filtro é aplicado - visibilidade total</li>
     *   <li>Paginação obrigatória para evitar sobrecarga de memória</li>
     *   <li>Ordenação por data de criação (mais recentes primeiro)</li>
     * </ul>
     * <p>
     * <b>Atenção:</b>
     * Este método pode retornar grandes volumes de dados em sistemas com histórico extenso.
     * Use com cautela em ambientes de produção e considere limitar o tamanho da página
     * para manter performance adequada.
     * <p>
     * <b>Recomendação:</b>
     * Para consultas em produção, prefira usar {@link #getWithFilters} com filtros apropriados
     * para reduzir o volume de dados e melhorar a experiência do usuário.
     *
     * @param page número da página a recuperar (zero-based)
     * @param size quantidade de registros por página (recomendado: 10-50 para UI, até 100 para APIs)
     * @return Page&lt;FraudAlertResponseDTO&gt; contendo todos os alertas do sistema
     */
    public Page<FraudAlertResponseDTO> getAllWithoutFilters(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FraudAlert> fraudAlerts = fraudAlertRepository.findAll(pageable);

        return fraudAlerts.map(FraudAlertResponseDTO::from);
    }
}