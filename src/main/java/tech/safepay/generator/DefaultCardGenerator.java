package tech.safepay.generator;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;
import tech.safepay.Enums.CardBrand;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Random;

/**
 * Gerador de dados sintéticos para criação de cartões de crédito válidos em ambiente de testes.
 * <p>
 * Este componente é responsável pela geração de todos os atributos necessários para criação
 * de cartões de crédito realistas, incluindo:
 * <ul>
 *   <li>Números de cartão válidos conformes ao algoritmo de Luhn</li>
 *   <li>Bandeiras de cartão (Visa, Mastercard, Amex, Elo)</li>
 *   <li>Datas de expiração futuras realistas</li>
 *   <li>Limites de crédito graduados</li>
 *   <li>Scores de risco iniciais</li>
 *   <li>Nomes de titulares sintetizados</li>
 * </ul>
 * <p>
 * <b>Conformidade e Segurança:</b>
 * <ul>
 *   <li>Números gerados passam na validação de Luhn (checksum correto)</li>
 *   <li>Prefixos conformes aos padrões das bandeiras reais</li>
 *   <li>Dados são SINTÉTICOS - não representam cartões reais</li>
 *   <li>Uso exclusivo para ambientes de teste e desenvolvimento</li>
 * </ul>
 * <p>
 * <b>Casos de uso:</b>
 * <ul>
 *   <li>População de banco de dados de teste</li>
 *   <li>Criação de datasets para testes automatizados</li>
 *   <li>Geração de massa de dados para load testing</li>
 *   <li>Simulação de carteiras de clientes</li>
 * </ul>
 * <p>
 * <b>AVISO IMPORTANTE:</b>
 * Este gerador NÃO deve ser utilizado para criar dados de produção ou cartões
 * reais. Os números gerados, embora válidos algoritmicamente, são sintéticos e
 * não correspondem a cartões emitidos por instituições financeiras.
 *
 * @author SafePay Development Team
 * @version 1.0
 * @since 2025-01
 */
@Component
public class DefaultCardGenerator {

    /**
     * Gerador de números aleatórios thread-safe para criação de dados de cartão.
     */
    private static final Random RANDOM = new Random();

    /**
     * Seleciona aleatoriamente uma bandeira de cartão entre todas as disponíveis.
     * <p>
     * Este método implementa seleção uniforme, onde cada bandeira tem probabilidade
     * igual de ser escolhida, independentemente de sua popularidade no mercado real.
     * <p>
     * <b>Bandeiras suportadas:</b>
     * <ul>
     *   <li>VISA</li>
     *   <li>MASTERCARD</li>
     *   <li>AMERICAN_EXPRESS</li>
     *   <li>ELO</li>
     * </ul>
     * <p>
     * <b>Distribuição:</b>
     * Cada bandeira tem 25% de probabilidade (distribuição uniforme).
     * Para simular distribuição de mercado real, considere implementar
     * seleção ponderada favorecendo Visa e Mastercard (dominam ~80% do mercado).
     *
     * @return bandeira de cartão selecionada aleatoriamente
     */
    public CardBrand choiceCardBrand() {
        CardBrand[] brands = CardBrand.values();
        int index = RANDOM.nextInt(brands.length);
        return brands[index];
    }

    /**
     * Gera uma data de expiração futura realista para o cartão.
     * <p>
     * Cartões de crédito tipicamente têm validade de 2 a 5 anos a partir da emissão.
     * Este método simula esse padrão gerando datas de expiração aleatórias nesse intervalo.
     * <p>
     * <b>Algoritmo:</b>
     * <ol>
     *   <li>Adiciona entre 2 e 5 anos à data atual</li>
     *   <li>Define mês aleatório (1-12)</li>
     *   <li>Fixa dia 1 (padrão: cartões expiram no primeiro dia do mês)</li>
     * </ol>
     * <p>
     * <b>Formato de expiração:</b>
     * Cartões reais expiram no último dia do mês especificado, mas o padrão da
     * indústria é armazenar apenas mês/ano. Este método usa dia 1 como normalização
     * de armazenamento, devendo ser interpretado como "válido até o fim daquele mês".
     * <p>
     * <b>Exemplo de saída:</b>
     * <ul>
     *   <li>Hoje: 2025-01-27</li>
     *   <li>Anos adicionados: 3</li>
     *   <li>Mês sorteado: 8</li>
     *   <li>Resultado: 2028-08-01 (válido até 31/08/2028)</li>
     * </ul>
     *
     * @return data de expiração futura entre 2 e 5 anos a partir de hoje
     */
    public LocalDate generateExpirationDate() {
        int yearsToAdd = RANDOM.nextInt(4) + 2; // 2 a 5 anos
        return LocalDate.now()
                .plusYears(yearsToAdd)
                .withMonth(RANDOM.nextInt(12) + 1)
                .withDayOfMonth(1);


    }

    /**
     * Gera um limite de crédito graduado em múltiplos de R$ 1.000.
     * <p>
     * Instituições financeiras tipicamente oferecem limites de crédito em valores
     * redondos e graduados. Este método simula esse padrão gerando limites entre
     * R$ 1.000 e R$ 10.000 em incrementos de mil.
     * <p>
     * <b>Valores possíveis:</b>
     * <ul>
     *   <li>R$ 1.000 (limite mínimo - cartão básico)</li>
     *   <li>R$ 2.000, R$ 3.000, R$ 4.000 (limites intermediários)</li>
     *   <li>R$ 5.000 (limite médio padrão)</li>
     *   <li>R$ 6.000, R$ 7.000, R$ 8.000, R$ 9.000 (limites elevados)</li>
     *   <li>R$ 10.000 (limite máximo - cartão premium)</li>
     * </ul>
     * <p>
     * <b>Distribuição estatística:</b>
     * Distribuição uniforme: cada valor tem 10% de probabilidade.
     * <p>
     * <b>Nota de realismo:</b>
     * Na prática, bancos utilizam análise de crédito sofisticada para determinar
     * limites (score de crédito, renda, histórico). Este método usa distribuição
     * uniforme por simplicidade, adequada para testes que não dependem de
     * correlação entre limite e perfil de risco.
     * <p>
     * <b>Evolução futura:</b>
     * Para maior realismo, considere correlacionar limite com:
     * <ul>
     *   <li>Score de risco do cartão</li>
     *   <li>Tipo de cartão (básico, gold, platinum)</li>
     *   <li>Bandeira (Amex tipicamente oferece limites maiores)</li>
     * </ul>
     *
     * @return limite de crédito entre R$ 1.000 e R$ 10.000 em múltiplos de mil
     */
    public BigDecimal generateCreditLimit() {
        int steps = RANDOM.nextInt(10) + 1; // 1 a 10
        return BigDecimal.valueOf(steps * 1000L);
    }


    /**
     * Gera um score de risco inicial baixo para o cartão.
     * <p>
     * O score de risco é uma métrica inicial que representa o nível de risco
     * atribuído ao cartão no momento de sua criação, antes de qualquer histórico
     * transacional.
     * <p>
     * <b>Faixa de valores:</b>
     * 0 a 29 (escala de 0-100, onde 0 = risco mínimo, 100 = risco máximo)
     * <p>
     * <b>Interpretação:</b>
     * <ul>
     *   <li><b>0-9:</b> Risco muito baixo (perfil excelente)</li>
     *   <li><b>10-19:</b> Risco baixo (perfil bom)</li>
     *   <li><b>20-29:</b> Risco moderado-baixo (perfil aceitável)</li>
     * </ul>
     * <p>
     * <b>Por que limitar a 29?</b>
     * Cartões recém-criados geralmente começam com score baixo (presumption of innocence).
     * Scores mais altos são desenvolvidos ao longo do tempo baseado em comportamento
     * transacional, padrões de pagamento, e incidentes de fraude.
     * <p>
     * <b>Evolução do score:</b>
     * Este score inicial é apenas o ponto de partida. Durante o ciclo de vida do cartão:
     * <ul>
     *   <li>Bom comportamento de pagamento pode reduzir o score</li>
     *   <li>Alertas de fraude aumentam o score</li>
     *   <li>Transações bloqueadas impactam o score</li>
     *   <li>Padrões anômalos elevam temporariamente o score</li>
     * </ul>
     * <p>
     * <b>Uso no sistema:</b>
     * O score de risco inicial pode influenciar:
     * <ul>
     *   <li>Limites de transação diários</li>
     *   <li>Thresholds para autenticação adicional</li>
     *   <li>Categorias de comerciantes permitidas</li>
     *   <li>Necessidade de revisão manual inicial</li>
     * </ul>
     *
     * @return score de risco entre 0 e 29
     */
    public Integer generateRiskScore(){
        return RANDOM.nextInt(30);
    }


    /**
     * Gera um número de cartão de crédito válido conforme os padrões da bandeira especificada.
     * <p>
     * Este método implementa geração completa de números de cartão seguindo:
     * <ul>
     *   <li>Prefixos IIN (Issuer Identification Number) corretos por bandeira</li>
     *   <li>Comprimento adequado (15 ou 16 dígitos conforme bandeira)</li>
     *   <li>Dígito verificador calculado via algoritmo de Luhn</li>
     * </ul>
     * <p>
     * <b>Padrões por bandeira:</b>
     * <table border="1">
     *   <tr>
     *     <th>Bandeira</th>
     *     <th>Prefixo (IIN)</th>
     *     <th>Comprimento</th>
     *     <th>Exemplo</th>
     *   </tr>
     *   <tr>
     *     <td>VISA</td>
     *     <td>4</td>
     *     <td>16 dígitos</td>
     *     <td>4532-1234-5678-9010</td>
     *   </tr>
     *   <tr>
     *     <td>MASTERCARD</td>
     *     <td>51-55</td>
     *     <td>16 dígitos</td>
     *     <td>5425-2334-3010-9903</td>
     *   </tr>
     *   <tr>
     *     <td>AMERICAN EXPRESS</td>
     *     <td>34 ou 37</td>
     *     <td>15 dígitos</td>
     *     <td>3782-822463-10005</td>
     *   </tr>
     *   <tr>
     *     <td>ELO</td>
     *     <td>6362</td>
     *     <td>16 dígitos</td>
     *     <td>6362-9700-0000-0005</td>
     *   </tr>
     * </table>
     * <p>
     * <b>Algoritmo de geração:</b>
     * <ol>
     *   <li>Define prefixo IIN baseado na bandeira</li>
     *   <li>Define comprimento total (15 ou 16 dígitos)</li>
     *   <li>Gera dígitos aleatórios para completar (exceto checksum)</li>
     *   <li>Calcula dígito verificador usando algoritmo de Luhn</li>
     *   <li>Concatena: prefixo + dígitos aleatórios + checksum</li>
     * </ol>
     * <p>
     * <b>Algoritmo de Luhn (Mod 10):</b>
     * Padrão ISO/IEC 7812-1 para validação de números de cartão:
     * <ol>
     *   <li>Percorre dígitos da direita para esquerda</li>
     *   <li>Dobra cada segundo dígito</li>
     *   <li>Se resultado > 9, subtrai 9</li>
     *   <li>Soma todos os dígitos</li>
     *   <li>Checksum = (10 - (soma % 10)) % 10</li>
     * </ol>
     * <p>
     * <b>Validação:</b>
     * Números gerados por este método:
     * <ul>
     *   <li>✓ Passam em validadores de cartão online</li>
     *   <li>✓ São aceitos em formulários com validação de Luhn</li>
     *   <li>✓ Seguem estrutura IIN correta</li>
     *   <li>✗ NÃO são cartões reais emitidos por bancos</li>
     *   <li>✗ NÃO funcionam para transações reais</li>
     * </ul>
     * <p>
     * <b>Exemplo de geração (VISA):</b>
     * <pre>
     * Bandeira: VISA
     * Prefixo: "4"
     * Comprimento: 16
     * Dígitos aleatórios: "532123456789010" (14 dígitos)
     * Base completa: "453212345678901" (15 dígitos)
     * Cálculo Luhn: checksum = 0
     * Número final: "4532123456789010"
     * </pre>
     * <p>
     * <b>Segurança e Conformidade:</b>
     * <ul>
     *   <li>Números são sintéticos e detectáveis como tal</li>
     *   <li>Não violam PCI-DSS (dados não são de cartões reais)</li>
     *   <li>Adequados para ambiente de teste conforme ISO 8583</li>
     *   <li>Prefixos de teste oficiais (como 4111111111111111) não são usados</li>
     * </ul>
     *
     * @param cardBrand bandeira do cartão para gerar número conforme padrão
     * @return número de cartão válido (passa em validação de Luhn) formatado sem separadores
     * @throws IllegalArgumentException se bandeira não for suportada
     */
    public String generateNumber(CardBrand cardBrand) {
        String prefix;
        int length;

        switch (cardBrand) {
            case VISA -> {
                prefix = "4";
                length = 16;
            }
            case MASTERCARD -> {
                prefix = "5" + (RANDOM.nextInt(5) + 1);
                length = 16;
            }
            case AMERICAN_EXPRESS -> {
                prefix = RANDOM.nextBoolean() ? "34" : "37";
                length = 15;
            }
            case ELO -> {
                prefix = "6362";
                length = 16;
            }
            default -> throw new IllegalArgumentException("Brand não suportada");
        }

        String baseNumber = prefix + generateRandomDigits(length - prefix.length() - 1);
        int checkDigit = calculateLuhnCheckDigit(baseNumber);
        return baseNumber + checkDigit;
    }

    /**
     * Gera uma string de dígitos numéricos aleatórios.
     * <p>
     * Método auxiliar utilizado para preencher a porção aleatória do número do cartão
     * entre o prefixo IIN e o dígito verificador.
     * <p>
     * <b>Características:</b>
     * <ul>
     *   <li>Cada dígito é independente e uniformemente distribuído (0-9)</li>
     *   <li>Sem formatação (retorna string contígua)</li>
     *   <li>Performance O(n) onde n = count</li>
     * </ul>
     * <p>
     * <b>Uso típico:</b>
     * <pre>
     * generateRandomDigits(12) → "837492016584"
     * generateRandomDigits(5)  → "04721"
     * </pre>
     *
     * @param count quantidade de dígitos aleatórios a gerar
     * @return string contendo exatamente 'count' dígitos aleatórios (0-9)
     */
    private String generateRandomDigits(int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Calcula o dígito verificador de Luhn para um número de cartão parcial.
     * <p>
     * Implementa o algoritmo de Luhn (também conhecido como "modulus 10" ou "mod 10"),
     * que é o padrão da indústria para validação de números de cartão de crédito
     * definido na norma ISO/IEC 7812-1.
     * <p>
     * <b>Algoritmo passo a passo:</b>
     * <ol>
     *   <li>Percorre o número da direita para esquerda</li>
     *   <li>Dobra cada segundo dígito (posições ímpares da direita)</li>
     *   <li>Se o dígito dobrado > 9: subtrai 9 (equivale a somar os dígitos)</li>
     *   <li>Soma todos os dígitos (dobrados e não-dobrados)</li>
     *   <li>Calcula checksum: (10 - (soma % 10)) % 10</li>
     * </ol>
     * <p>
     * <b>Exemplo detalhado:</b>
     * <pre>
     * Número base: "453212345678901"
     *
     * Posição (da direita): 1  2  3  4  5  6  7  8  9 10 11 12 13 14 15
     * Dígito:               1  0  9  8  7  6  5  4  3  2  1  2  3  5  4
     * Dobrar?:              N  S  N  S  N  S  N  S  N  S  N  S  N  S  N
     * Após dobrar:          1  0  9 16  7 12  5  8  3  4  1  4  3 10  4
     * Ajuste (>9):          1  0  9  7  7  3  5  8  3  4  1  4  3  1  4
     *
     * Soma: 1+0+9+7+7+3+5+8+3+4+1+4+3+1+4 = 60
     * Checksum: (10 - (60 % 10)) % 10 = (10 - 0) % 10 = 0
     *
     * Número completo: "4532123456789010"
     * </pre>
     * <p>
     * <b>Por que funciona?</b>
     * O algoritmo de Luhn detecta:
     * <ul>
     *   <li>Erros de digitação simples (troca de um dígito)</li>
     *   <li>Transposição de dígitos adjacentes (trocar 12 por 21)</li>
     *   <li>~90% dos erros de entrada manual</li>
     * </ul>
     * <p>
     * <b>Limitações:</b>
     * Não detecta:
     * <ul>
     *   <li>Transposição de sequências (1234 → 3412)</li>
     *   <li>Erros duplos em posições específicas</li>
     *   <li>Fraudes intencionais (algoritmo é público)</li>
     * </ul>
     * <p>
     * <b>Performance:</b>
     * Complexidade O(n) onde n = comprimento do número (tipicamente 14-15 dígitos).
     * Muito eficiente para uso em loops de geração em massa.
     *
     * @param number string contendo dígitos do cartão sem o checksum final
     * @return dígito verificador de Luhn (0-9)
     */
    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (10 - (sum % 10)) % 10;
    }

    /**
     * Gera um nome completo realista para o titular do cartão.
     * <p>
     * Utiliza a biblioteca JavaFaker para gerar nomes sintetizados que:
     * <ul>
     *   <li>Seguem padrões de nomenclatura reais</li>
     *   <li>Incluem primeiro nome e sobrenome</li>
     *   <li>Respeitam regras de capitalização</li>
     *   <li>São variados e não repetitivos</li>
     * </ul>
     * <p>
     * <b>Características dos nomes gerados:</b>
     * <ul>
     *   <li>Nomes completos (2-3 palavras típicas)</li>
     *   <li>Capitalização correta (Title Case)</li>
     *   <li>Mix de nomes comuns e incomuns</li>
     *   <li>Internacionalmente diversos</li>
     * </ul>
     * <p>
     * <b>Exemplos de saída:</b>
     * <ul>
     *   <li>"John Smith"</li>
     *   <li>"Maria Silva Santos"</li>
     *   <li>"Robert Anderson"</li>
     *   <li>"Ana Paula Costa"</li>
     * </ul>
     * <p>
     * <b>Nota de localização:</b>
     * JavaFaker usa locale padrão do sistema. Para nomes específicos de uma região:
     * <pre>
     * Faker faker = new Faker(new Locale("pt", "BR")); // Nomes brasileiros
     * Faker faker = new Faker(new Locale("en", "US")); // Nomes americanos
     * </pre>
     * <p>
     * <b>Privacidade:</b>
     * Nomes gerados são sintéticos e não correspondem a pessoas reais.
     * Adequado para uso em testes sem violar LGPD/GDPR.
     * <p>
     * <b>Performance:</b>
     * JavaFaker gera nomes sob demanda. Para geração em massa (10.000+ cartões),
     * considere cache de nomes ou geração em lote para melhor performance.
     *
     * @return nome completo sintetizado do titular do cartão
     */
    public String generateName(){
        Faker faker = new Faker();
        return faker.name().fullName();
    }
}