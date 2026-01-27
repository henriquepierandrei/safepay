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

/**
 * Repositório responsável pelo acesso e gerenciamento
 * de dados relacionados a cartões.
 *
 * <p>Centraliza consultas transacionais e analíticas
 * utilizadas por módulos de negócio, risco e antifraude,
 * oferecendo suporte a filtros dinâmicos e paginação.</p>
 */
@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Recupera todos os cartões que possuem pelo menos
     * um dispositivo vinculado.
     *
     * <p>Utilizado em análises de relacionamento cartão–dispositivo
     * e cenários de correlação antifraude.</p>
     *
     * @return lista de cartões com dispositivos associados
     */
    List<Card> findByDevicesIsNotEmpty();

    /**
     * Realiza uma busca paginada de cartões aplicando filtros opcionais.
     *
     * <p>Permite filtrar por bandeira do cartão e por critério
     * de criação recente, mantendo flexibilidade para
     * consultas administrativas e operacionais.</p>
     *
     * <p>Quando {@code recentlyCreated} é verdadeiro, retorna cartões
     * criados a partir da data limite. Quando falso, retorna cartões
     * anteriores à data limite. Caso seja nulo, o filtro é ignorado.</p>
     *
     * @param brand bandeira do cartão (opcional)
     * @param recentlyCreated indicador de criação recente (opcional)
     * @param limitDate data de referência para avaliação temporal
     * @param pageable parâmetros de paginação e ordenação
     * @return página de cartões conforme os filtros aplicados
     */
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
