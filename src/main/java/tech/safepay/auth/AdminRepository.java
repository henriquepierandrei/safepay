package tech.safepay.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JPA para gerenciamento de entidades de administradores.
 * <p>
 * Fornece operações CRUD padrão herdadas de {@link JpaRepository} e queries
 * customizadas para busca e validação de administradores por email.
 * </p>
 * <p>
 * Este repositório é a camada de acesso a dados para o sistema de autenticação
 * administrativa, permitindo operações de persistência e consulta de admins.
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see AdminEntity
 * @see AuthService
 */
@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, UUID> {

    /**
     * Busca um administrador pelo endereço de email.
     * <p>
     * Este método é fundamental para o processo de autenticação, permitindo
     * localizar o admin durante o login pela credencial única (email).
     * </p>
     * <p>
     * <b>Query derivada automaticamente pelo Spring Data JPA:</b><br>
     * {@code SELECT * FROM admins WHERE email = ?}
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Validação de credenciais durante login</li>
     *   <li>Verificação de existência de admin por email</li>
     *   <li>Recuperação de senha (buscar admin para envio de email)</li>
     *   <li>Atualização de dados de admin específico</li>
     * </ul>
     * </p>
     *
     * @param email endereço de email do administrador (deve ser único no sistema)
     * @return {@link Optional} contendo o admin se encontrado, ou vazio caso não exista
     *
     * @implNote O email deve ser validado e normalizado (lowercase) antes da busca
     *           para evitar inconsistências. O campo possui constraint de unicidade
     *           no banco de dados, garantindo no máximo um resultado.
     *
     * @see AuthService#login(AuthService.LoginRequest)
     */
    Optional<AdminEntity> findByEmail(String email);

    /**
     * Verifica se existe um administrador cadastrado com o email especificado.
     * <p>
     * Método otimizado para verificação de existência sem carregar a entidade completa.
     * Mais eficiente que {@link #findByEmail(String)} quando apenas a existência
     * precisa ser confirmada.
     * </p>
     * <p>
     * <b>Query gerada:</b><br>
     * {@code SELECT COUNT(*) > 0 FROM admins WHERE email = ?}
     * </p>
     * <p>
     * <b>Casos de uso:</b>
     * <ul>
     *   <li>Validação durante cadastro de novo admin (evitar duplicação)</li>
     *   <li>Verificação de disponibilidade de email</li>
     *   <li>Validações em APIs REST antes de operações complexas</li>
     *   <li>Checks de segurança em fluxos de redefinição de senha</li>
     * </ul>
     * </p>
     * <p>
     * <b>Vantagem de performance:</b> Não carrega toda a entidade na memória,
     * apenas executa COUNT no banco de dados, tornando a operação muito mais leve.
     * </p>
     *
     * @param email endereço de email a ser verificado
     * @return {@code true} se existe um admin com este email, {@code false} caso contrário
     *
     * @implNote Útil para validações pré-operação onde apenas a existência importa,
     *           economizando memória e reduzindo tráfego de dados entre aplicação e banco
     */
    boolean existsByEmail(String email);
}