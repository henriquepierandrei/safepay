package tech.safepay.auth;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Classe de configuração responsável por popular o banco de dados com dados iniciais.
 * <p>
 * Cria automaticamente um administrador padrão na primeira execução da aplicação,
 * permitindo acesso inicial ao sistema sem necessidade de inserção manual no banco.
 * </p>
 * <p>
 * <b>⚠️ ATENÇÃO - SEGURANÇA EM PRODUÇÃO:</b>
 * <ul>
 *   <li>As credenciais padrão DEVEM ser alteradas imediatamente após primeiro acesso</li>
 *   <li>Considere desabilitar este seeder em produção ou usar variáveis de ambiente</li>
 *   <li>Nunca commite credenciais reais no código-fonte</li>
 * </ul>
 * </p>
 *
 * @author SafePay Team
 * @since 1.0
 * @see CommandLineRunner
 * @see AdminEntity
 */
@Configuration
public class AdminSeeder {

    /**
     * Cria um {@link CommandLineRunner} que executa automaticamente na inicialização da aplicação.
     * <p>
     * Este bean verifica se já existe um administrador com email padrão e, caso não exista,
     * cria um novo admin com credenciais pré-definidas.
     * </p>
     * <p>
     * <b>Fluxo de execução:</b>
     * <ol>
     *   <li>Verifica se já existe admin com email "admin@safepay.tech"</li>
     *   <li>Se NÃO existir, cria nova instância de {@link AdminEntity}</li>
     *   <li>Define email padrão: {@code admin@safepay.tech}</li>
     *   <li>Criptografa senha padrão ({@code Admin@123}) usando BCrypt</li>
     *   <li>Persiste o admin no banco de dados</li>
     *   <li>Exibe mensagem de confirmação no console</li>
     * </ol>
     * </p>
     * <p>
     * <b>Quando executa:</b> Automaticamente ao iniciar a aplicação Spring Boot,
     * após a inicialização completa do contexto e conexão com o banco de dados.
     * </p>
     * <p>
     * <b>Idempotência:</b> Pode ser executado múltiplas vezes sem efeitos colaterais.
     * A verificação {@code existsByEmail} garante que apenas um admin padrão seja criado.
     * </p>
     * <p>
     * <b>⚠️ RECOMENDAÇÕES DE SEGURANÇA PARA PRODUÇÃO:</b>
     * <pre>
     * // Opção 1: Usar variáveis de ambiente
     * String adminEmail = System.getenv("ADMIN_EMAIL");
     * String adminPassword = System.getenv("ADMIN_PASSWORD");
     *
     * // Opção 2: Desabilitar em produção
     * {@literal @}Profile("!production")
     * {@literal @}Bean
     * CommandLineRunner initAdmin(...) { ... }
     *
     * // Opção 3: Usar senha aleatória gerada
     * String randomPassword = UUID.randomUUID().toString();
     * // Log seguro ou envio por email
     * </pre>
     * </p>
     *
     * @param repository repositório JPA para operações de persistência de admins
     * @param encoder encoder BCrypt para criptografia segura da senha
     * @return {@link CommandLineRunner} que será executado automaticamente na inicialização
     *
     * @implNote A senha é criptografada usando BCrypt com salt aleatório, garantindo
     *           que mesmo senhas idênticas geram hashes diferentes. O custo padrão
     *           do BCrypt (10 rounds) oferece bom balanço entre segurança e performance.
     *
     * @see PasswordEncoder#encode(CharSequence)
     * @see AdminRepository#existsByEmail(String)
     */
    @Bean
    CommandLineRunner initAdmin(AdminRepository repository, PasswordEncoder encoder) {
        return args -> {
            // Verifica se já existe um admin com o email padrão
            if (!repository.existsByEmail("admin@safepay.tech")) {
                // Cria nova entidade de administrador
                AdminEntity admin = new AdminEntity();
                admin.setEmail("admin@safepay.tech");

                // Criptografa a senha usando BCrypt antes de armazenar
                // NUNCA armazene senhas em texto plano no banco de dados
                admin.setHashPassword(encoder.encode("Admin@123"));

                // Persiste o admin no banco de dados
                repository.save(admin);

                // Log de confirmação (visível apenas em desenvolvimento)
                // ⚠️ Em produção, evite logar credenciais mesmo que padrão
                System.out.println("✅ Admin criado: admin@safepay.tech / Admin@123");
            }
        };
    }
}