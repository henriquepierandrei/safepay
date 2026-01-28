package tech.safepay.validations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tech.safepay.dtos.validation.ValidationResultDto;
import tech.safepay.entities.Transaction;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Componente orquestrador central de todas as validações de transação.
 * <p>
 * Esta classe é responsável por coordenar a execução paralela e assíncrona de todas
 * as validações de fraude e risco disponíveis no sistema, consolidando os resultados
 * em uma única pontuação e conjunto de alertas.
 * </p>
 * <p>
 * <strong>Arquitetura de Validação:</strong>
 * <ol>
 *   <li><strong>Carregamento de Contexto:</strong> Recupera histórico de transações necessário</li>
 *   <li><strong>Criação de Snapshot:</strong> Cria estrutura imutável com dados históricos</li>
 *   <li><strong>Execução Paralela:</strong> Dispara todas as validações de forma assíncrona</li>
 *   <li><strong>Consolidação:</strong> Agrega pontuações e alertas de todas as validações</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Categorias de Validação Incluídas:</strong>
 * <ul>
 *   <li>Modelos externos de detecção de anomalias</li>
 *   <li>Padrões conhecidos de fraude</li>
 *   <li>Frequência e velocidade de transações</li>
 *   <li>Limites e valores</li>
 *   <li>Geolocalização e viagens impossíveis</li>
 *   <li>Rede e dispositivos</li>
 *   <li>Riscos operacionais</li>
 *   <li>Comportamento do usuário</li>
 *   <li>Limites e expiração de cartão</li>
 * </ul>
 * </p>
 *
 * @author SafePay Security Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class TransactionGlobalValidation {

    /**
     * Logger para registro de eventos e diagnóstico.
     */
    private static final Logger log = LoggerFactory.getLogger(TransactionGlobalValidation.class);

    /**
     * Simulador de modelo externo de detecção de fraude.
     */
    private final ExternalAntifraudModelSimulation externalAntifraudModelSimulation;

    /**
     * Validador de padrões conhecidos de fraude.
     */
    private final FraudPatternsValidation fraudPatternsValidation;

    /**
     * Validador de frequência e velocidade de transações.
     */
    private final FrequencyAndVelocityValidation frequencyAndVelocityValidation;

    /**
     * Validador de limites e valores de transações.
     */
    private final LimitAndAmountValidation limitAndAmountValidation;

    /**
     * Validador de geolocalização e anomalias de localização.
     */
    private final LocationValidation locationValidation;

    /**
     * Validador de rede e dispositivos.
     */
    private final NetworkAndDeviceValidation networkAndDeviceValidation;

    /**
     * Validador de riscos operacionais.
     */
    private final OperationalRiskValidation operationalRiskValidation;

    /**
     * Validador de comportamento do usuário.
     */
    private final UserBehaviorValidation userBehaviorValidation;

    /**
     * Validador de limites e expiração de cartão.
     */
    private final LimitAndExpirationValidation limitAndExpirationValidation;

    /**
     * Contexto de validação para gerenciamento de dados históricos.
     */
    private final ValidationContext context;

    /**
     * Executor para processamento paralelo de validações.
     * <p>
     * Configurado para permitir execução assíncrona sem compartilhamento
     * de contexto request-scoped.
     * </p>
     */
    private final Executor validationExecutor;

    /**
     * Construtor para injeção de dependências.
     *
     * @param context                            contexto de validação
     * @param externalAntifraudModelSimulation   simulador de modelo externo
     * @param fraudPatternsValidation            validador de padrões de fraude
     * @param frequencyAndVelocityValidation     validador de frequência e velocidade
     * @param limitAndAmountValidation           validador de limites e valores
     * @param locationValidation                 validador de localização
     * @param networkAndDeviceValidation         validador de rede e dispositivos
     * @param operationalRiskValidation          validador de riscos operacionais
     * @param userBehaviorValidation             validador de comportamento
     * @param limitAndExpirationValidation       validador de expiração
     * @param validationExecutor                 executor para processamento paralelo
     */
    public TransactionGlobalValidation(
            ValidationContext context,
            ExternalAntifraudModelSimulation externalAntifraudModelSimulation,
            FraudPatternsValidation fraudPatternsValidation,
            FrequencyAndVelocityValidation frequencyAndVelocityValidation,
            LimitAndAmountValidation limitAndAmountValidation,
            LocationValidation locationValidation,
            NetworkAndDeviceValidation networkAndDeviceValidation,
            OperationalRiskValidation operationalRiskValidation,
            UserBehaviorValidation userBehaviorValidation,
            LimitAndExpirationValidation limitAndExpirationValidation,
            @Qualifier("validationExecutor") Executor validationExecutor
    ) {
        this.context = context;
        this.externalAntifraudModelSimulation = externalAntifraudModelSimulation;
        this.fraudPatternsValidation = fraudPatternsValidation;
        this.frequencyAndVelocityValidation = frequencyAndVelocityValidation;
        this.limitAndAmountValidation = limitAndAmountValidation;
        this.locationValidation = locationValidation;
        this.networkAndDeviceValidation = networkAndDeviceValidation;
        this.operationalRiskValidation = operationalRiskValidation;
        this.userBehaviorValidation = userBehaviorValidation;
        this.limitAndExpirationValidation = limitAndExpirationValidation;
        this.validationExecutor = validationExecutor;
    }

    /**
     * Snapshot imutável do histórico de transações para uso em validações assíncronas.
     * <p>
     * Esta estrutura de dados imutável contém diferentes janelas temporais do histórico
     * de transações, permitindo que validações paralelas acessem os dados de forma segura
     * sem preocupações de concorrência ou modificação durante o processamento.
     * </p>
     * <p>
     * <strong>Janelas Disponíveis:</strong>
     * <ul>
     *   <li><strong>last20:</strong> Últimas 20 transações do cartão</li>
     *   <li><strong>last10:</strong> Últimas 10 transações do cartão</li>
     *   <li><strong>last24Hours:</strong> Todas as transações das últimas 24 horas</li>
     *   <li><strong>last10Minutes:</strong> Todas as transações dos últimos 10 minutos</li>
     *   <li><strong>last5Minutes:</strong> Todas as transações dos últimos 5 minutos</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Garantias de Imutabilidade:</strong>
     * <br>
     * O uso de record Java garante imutabilidade, tornando esta estrutura thread-safe
     * para uso em validações paralelas sem necessidade de sincronização.
     * </p>
     *
     * @param last20        últimas 20 transações
     * @param last10        últimas 10 transações
     * @param last24Hours   transações das últimas 24 horas
     * @param last10Minutes transações dos últimos 10 minutos
     * @param last5Minutes  transações dos últimos 5 minutos
     */
    public record ValidationSnapshot(
            List<Transaction> last20,
            List<Transaction> last10,
            List<Transaction> last24Hours,
            List<Transaction> last10Minutes,
            List<Transaction> last5Minutes
    ) {}

    /**
     * Executa todas as validações disponíveis de forma paralela e consolida os resultados.
     * <p>
     * Este método é o ponto de entrada principal do sistema de validação, orquestrando
     * a execução assíncrona de todas as validações e agregando suas pontuações e alertas
     * em um resultado único.
     * </p>
     * <p>
     * <strong>Fluxo de Execução:</strong>
     * </p>
     * <p>
     * <strong>1. Carregamento de Contexto (Thread HTTP Principal):</strong>
     * <br>
     * Carrega o histórico de transações necessário a partir do banco de dados,
     * executado de forma síncrona na thread da requisição HTTP.
     * </p>
     * <p>
     * <strong>2. Criação de Snapshot Imutável:</strong>
     * <br>
     * Cria estrutura de dados imutável contendo diferentes janelas temporais
     * do histórico, permitindo acesso seguro em threads paralelas.
     * </p>
     * <p>
     * <strong>3. Execução Paralela de Validações:</strong>
     * <br>
     * Dispara todas as validações usando CompletableFuture no executor configurado.
     * Cada validação executa de forma independente sem compartilhar estado mutável.
     * Total de 19 validações executadas em paralelo:
     * <ul>
     *   <li>1 validação de modelo externo de anomalia</li>
     *   <li>3 validações de padrões de fraude</li>
     *   <li>2 validações de frequência/velocidade</li>
     *   <li>2 validações de limite/valor</li>
     *   <li>3 validações de localização</li>
     *   <li>4 validações de rede/dispositivo</li>
     *   <li>2 validações de risco operacional</li>
     *   <li>1 validação de comportamento de usuário</li>
     *   <li>1 validação de limite/expiração</li>
     * </ul>
     * </p>
     * <p>
     * <strong>4. Aguardo de Conclusão:</strong>
     * <br>
     * Usa {@code CompletableFuture.allOf()} para aguardar a conclusão de todas
     * as validações antes de prosseguir.
     * </p>
     * <p>
     * <strong>5. Consolidação de Resultados:</strong>
     * <br>
     * Agrega as pontuações e alertas de todas as validações em um único resultado,
     * somando scores e consolidando a lista de alertas disparados.
     * </p>
     * <p>
     * <strong>Características Importantes:</strong>
     * <ul>
     *   <li>Todas as validações são executadas, mesmo se alguma falhar</li>
     *   <li>Não há short-circuit - sistema sempre calcula pontuação completa</li>
     *   <li>Thread-safe através de dados imutáveis e executor isolado</li>
     *   <li>Não depende de RequestScope nas threads de validação</li>
     * </ul>
     * </p>
     *
     * @param transaction a transação sendo validada
     * @return {@link ValidationResultDto} contendo a pontuação total agregada e lista
     *         completa de todos os alertas disparados por qualquer validação
     * @see ValidationSnapshot
     * @see ValidationContext#loadContext(Transaction)
     * @see CompletableFuture
     */
    public ValidationResultDto validateAll(Transaction transaction) {

        // 1️⃣ Carrega contexto NA THREAD HTTP
        context.loadContext(transaction);

        ValidationSnapshot snapshot = new ValidationSnapshot(
                context.getLast20Transactions(),
                context.getLast10Transactions(),
                context.getLast24Hours(),
                context.getLast10Minutes(),
                context.getLast5Minutes()
        );

        ValidationResultDto finalResult = new ValidationResultDto();

        // 2️⃣ Execução paralela (SEM RequestScope)
        List<CompletableFuture<ValidationResultDto>> futures = List.of(

                CompletableFuture.supplyAsync(
                        () -> externalAntifraudModelSimulation.anomalyModelTriggered(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.cardTestingPattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.microTransactionPattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> frequencyAndVelocityValidation.velocityAbuseValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> frequencyAndVelocityValidation.burstActivityValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> fraudPatternsValidation.declineThenApprovePattern(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndAmountValidation.highAmountValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndAmountValidation.limitExceededValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.impossibleTravelValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.highRiskCountryValidation(transaction),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> locationValidation.locationAnomalyValidation(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.newDeviceDetected(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.deviceFingerprintChange(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.torOrProxyDetected(transaction),
                        validationExecutor
                ),
                CompletableFuture.supplyAsync(
                        () -> networkAndDeviceValidation.multipleCardsSameDevice(transaction),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> operationalRiskValidation.multipleFailedAttempts(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> operationalRiskValidation.suspiciousSuccessAfterFailure(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> userBehaviorValidation.timeOfDayAnomaly(transaction, snapshot),
                        validationExecutor
                ),

                CompletableFuture.supplyAsync(
                        () -> limitAndExpirationValidation.validate(transaction),
                        validationExecutor
                )
        );

        // 3️⃣ Aguarda tudo
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 4️⃣ Consolidação
        futures.stream()
                .map(CompletableFuture::join)
                .forEach(r -> {
                    finalResult.addScore(r.getScore());
                    r.getTriggeredAlerts().forEach(finalResult::addAlert);
                });

        return finalResult;
    }
}